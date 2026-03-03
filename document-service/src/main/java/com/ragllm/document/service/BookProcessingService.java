package com.ragllm.document.service;

import com.ragllm.common.entity.Book;
import com.ragllm.common.entity.Chapter;
import com.ragllm.common.entity.Chunk;
import com.ragllm.common.exception.DocumentProcessingException;
import com.ragllm.common.tenant.TenantContext;
import com.ragllm.document.repository.BookRepository;
import com.ragllm.document.repository.ChapterRepository;
import com.ragllm.document.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Separate service for async book processing to ensure Spring's proxy-based @Async works.
 * Uses programmatic transactions (TransactionTemplate) so that status updates
 * (PROCESSING / COMPLETED / FAILED) are committed independently of the main work.
 */
@Service
public class BookProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BookProcessingService.class);

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final ChunkRepository chunkRepository;
    private final PdfProcessingService pdfProcessingService;
    private final EmbeddingClientService embeddingClientService;
    private final TransactionTemplate txTemplate;

    public BookProcessingService(BookRepository bookRepository,
                                  ChapterRepository chapterRepository,
                                  ChunkRepository chunkRepository,
                                  PdfProcessingService pdfProcessingService,
                                  EmbeddingClientService embeddingClientService,
                                  TransactionTemplate txTemplate) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.chunkRepository = chunkRepository;
        this.pdfProcessingService = pdfProcessingService;
        this.embeddingClientService = embeddingClientService;
        this.txTemplate = txTemplate;
    }

    @Async("documentExecutor")
    public void processBookAsync(UUID bookId, UUID tenantId) {
        TenantContext.setCurrentTenantId(tenantId);
        try {
            // ── Transaction 1: set status to PROCESSING (committed immediately) ──
            String filePath = txTemplate.execute(status -> {
                Book book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new DocumentProcessingException("Book not found: " + bookId));
                book.setProcessingStatus(Book.ProcessingStatus.PROCESSING);
                bookRepository.save(book);
                log.info("Book {} status set to PROCESSING", bookId);
                return book.getFilePath();
            });

            // ── Extract text (no transaction needed for file I/O) ──
            Path path = Path.of(filePath);
            List<PdfProcessingService.PageContent> pages;
            try (InputStream is = Files.newInputStream(path)) {
                pages = pdfProcessingService.extractTextByPage(is);
            }
            log.info("Book {} extracted {} pages", bookId, pages.size());

            // Chunk the text
            List<PdfProcessingService.TextChunk> textChunks = pdfProcessingService.chunkByHeading(pages);
            log.info("Book {} produced {} text chunks", bookId, textChunks.size());

            // ── Transaction 2: save chapters, chunks, embeddings, mark COMPLETED ──
            txTemplate.executeWithoutResult(status -> {
                Book book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new DocumentProcessingException("Book not found: " + bookId));
                book.setTotalPages(pages.size());

                // Create a default chapter for the book
                Chapter chapter = Chapter.builder()
                        .bookId(bookId)
                        .title(book.getName())
                        .chapterNumber(1)
                        .build();
                chapter = chapterRepository.save(chapter);
                log.info("Book {} chapter saved: {}", bookId, chapter.getId());

                // Generate embeddings and save chunks
                for (int i = 0; i < textChunks.size(); i++) {
                    PdfProcessingService.TextChunk textChunk = textChunks.get(i);
                    log.info("Book {} generating embedding for chunk {}/{}", bookId, i + 1, textChunks.size());

                    float[] embedding = embeddingClientService.generateEmbedding(textChunk.content());

                    Chunk chunk = Chunk.builder()
                            .tenantId(tenantId)
                            .chapterId(chapter.getId())
                            .content(textChunk.content())
                            .embedding(embedding)
                            .pageNumber(textChunk.pageNumber())
                            .chunkIndex(textChunk.chunkIndex())
                            .build();
                    chunkRepository.save(chunk);
                }

                book.setProcessingStatus(Book.ProcessingStatus.COMPLETED);
                bookRepository.save(book);
            });

            log.info("Book processing completed: {} ({} chunks)", bookId, textChunks.size());

        } catch (Exception e) {
            log.error("Failed to process book: {}", bookId, e);
            // ── Separate transaction for FAILED status so it always commits ──
            try {
                txTemplate.executeWithoutResult(status -> {
                    bookRepository.findById(bookId).ifPresent(book -> {
                        book.setProcessingStatus(Book.ProcessingStatus.FAILED);
                        bookRepository.save(book);
                    });
                });
                log.info("Book {} status set to FAILED", bookId);
            } catch (Exception ex) {
                log.error("Failed to update book status to FAILED: {}", bookId, ex);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
