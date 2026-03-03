package com.ragllm.document.service;

import com.ragllm.common.dto.BookUploadResponse;
import com.ragllm.common.entity.Book;
import com.ragllm.common.exception.DocumentProcessingException;
import com.ragllm.common.tenant.TenantContext;
import com.ragllm.document.repository.BookRepository;
import com.ragllm.document.repository.ChapterRepository;
import com.ragllm.document.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final ChunkRepository chunkRepository;
    private final BookProcessingService bookProcessingService;

    public DocumentIngestionService(BookRepository bookRepository,
                                     ChapterRepository chapterRepository,
                                     ChunkRepository chunkRepository,
                                     BookProcessingService bookProcessingService) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.chunkRepository = chunkRepository;
        this.bookProcessingService = bookProcessingService;
    }

    /**
     * Upload and start processing a document.
     * Returns immediately; processing happens asynchronously.
     */
    @Transactional
    public BookUploadResponse uploadBook(MultipartFile file, String bookName,
                                          String subject, Integer classLevel,
                                          String embeddingModel) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Save file to storage
        String filePath = saveFile(file, tenantId);

        // Create book record
        Book book = Book.builder()
                .tenantId(tenantId)
                .name(bookName)
                .subject(subject)
                .classLevel(classLevel)
                .filePath(filePath)
                .processingStatus(Book.ProcessingStatus.PENDING)
                .build();

        book = bookRepository.save(book);
        log.info("Book uploaded: {} for tenant: {} (embeddingModel={})", bookName, tenantId, embeddingModel);

        // Defer async processing until AFTER the transaction commits,
        // so the book row is visible to the async thread's new transaction.
        final UUID bookId = book.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                bookProcessingService.processBookAsync(bookId, tenantId);
            }
        });

        return BookUploadResponse.builder()
                .bookId(book.getId().toString())
                .name(bookName)
                .status("PENDING")
                .message("Book uploaded. Processing will begin shortly.")
                .build();
    }

    /**
     * Delete a book and all associated chapters, chunks, embeddings, and file.
     */
    @Transactional
    public void deleteBook(UUID bookId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Book book = bookRepository.findByIdAndTenantId(bookId, tenantId)
                .orElseThrow(() -> new DocumentProcessingException("Book not found or access denied"));

        log.info("Deleting book '{}' (id={}) for tenant {}", book.getName(), bookId, tenantId);

        // Delete all chunks for this book in one query
        chunkRepository.deleteAllByBookId(bookId);
        // Delete chapters
        chapterRepository.deleteAllByBookId(bookId);
        // Delete the book record
        bookRepository.delete(book);

        // Try to delete the file from disk
        try {
            if (book.getFilePath() != null) {
                Files.deleteIfExists(Path.of(book.getFilePath()));
            }
        } catch (Exception e) {
            log.warn("Failed to delete file {}: {}", book.getFilePath(), e.getMessage());
        }

        log.info("Book '{}' and all associated data deleted successfully", book.getName());
    }

    @Transactional(readOnly = true)
    public List<Book> getBooks() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return bookRepository.findAllByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Book getBookById(UUID bookId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return bookRepository.findByIdAndTenantId(bookId, tenantId)
                .orElseThrow(() -> new DocumentProcessingException("Book not found"));
    }

    private String saveFile(MultipartFile file, UUID tenantId) {
        try {
            Path uploadDir = Path.of("uploads", tenantId.toString());
            Files.createDirectories(uploadDir);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to save uploaded file", e);
        }
    }
}
