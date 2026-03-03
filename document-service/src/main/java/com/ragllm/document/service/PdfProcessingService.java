package com.ragllm.document.service;

import com.ragllm.common.exception.DocumentProcessingException;
import com.ragllm.common.util.InputSanitizer;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts text from PDF files (both digital and scanned) and splits into meaningful chunks.
 * Uses PDFBox for digital text extraction and Tesseract OCR for scanned pages.
 */
@Service
public class PdfProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PdfProcessingService.class);
    private static final int MAX_CHUNK_SIZE = 1500;     // characters
    private static final int MIN_CHUNK_SIZE = 100;      // characters
    private static final int CHUNK_OVERLAP = 200;       // overlap for context continuity

    /**
     * Minimum number of characters a page must have from text extraction
     * to be considered a "digital" (non-scanned) page.
     * Below this threshold, OCR will be attempted.
     */
    private static final int MIN_TEXT_LENGTH_THRESHOLD = 30;

    /**
     * OCR rendering DPI — higher = better accuracy but slower.
     */
    private static final int OCR_DPI = 300;

    // Patterns for detecting chapter/section headings
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(Chapter\\s+\\d+|CHAPTER\\s+\\d+|\\d+\\.\\d+\\s+[A-Z]|UNIT\\s+\\d+|Section\\s+\\d+)",
            Pattern.MULTILINE
    );

    private final String tessdataPath;
    private final boolean ocrEnabled;

    public PdfProcessingService(
            @Value("${ocr.tessdata-path:#{null}}") String tessdataPath,
            @Value("${ocr.enabled:true}") boolean ocrEnabled) {
        // Auto-detect tessdata from TESSDATA_PREFIX env var if not configured
        if (tessdataPath == null || tessdataPath.isBlank()) {
            String envPath = System.getenv("TESSDATA_PREFIX");
            this.tessdataPath = (envPath != null && !envPath.isBlank()) ? envPath : "/usr/share/tessdata";
        } else {
            this.tessdataPath = tessdataPath;
        }
        this.ocrEnabled = ocrEnabled;
        log.info("PdfProcessingService initialized — OCR enabled={}, tessdata={}", ocrEnabled, this.tessdataPath);
    }

    /**
     * Extract text from PDF input stream.
     * Falls back to OCR for scanned pages.
     */
    public String extractText(InputStream pdfInputStream) {
        try (PDDocument document = Loader.loadPDF(pdfInputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // If very little text extracted, try OCR on the whole document
            if (text.trim().length() < MIN_TEXT_LENGTH_THRESHOLD && ocrEnabled) {
                log.info("Digital text extraction yielded only {} chars — attempting OCR on entire document",
                        text.trim().length());
                text = ocrEntireDocument(document);
            }

            log.info("Extracted {} characters from PDF ({} pages)", text.length(), document.getNumberOfPages());
            return InputSanitizer.sanitizeContent(text);
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Extract text page by page from PDF.
     * Automatically detects scanned pages and uses OCR as fallback.
     */
    public List<PageContent> extractTextByPage(InputStream pdfInputStream) {
        List<PageContent> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfInputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = ocrEnabled ? new PDFRenderer(document) : null;
            int totalPages = document.getNumberOfPages();
            int ocrPageCount = 0;

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);

                // If this page has very little text, it may be scanned — try OCR
                if (text.trim().length() < MIN_TEXT_LENGTH_THRESHOLD && ocrEnabled && renderer != null) {
                    log.debug("Page {} has only {} chars of digital text — running OCR", page, text.trim().length());
                    String ocrText = ocrSinglePage(renderer, page - 1); // renderer is 0-indexed
                    if (ocrText != null && ocrText.trim().length() > text.trim().length()) {
                        text = ocrText;
                        ocrPageCount++;
                    }
                }

                text = InputSanitizer.sanitizeContent(text);
                if (!text.isBlank()) {
                    pages.add(new PageContent(page, text));
                }
            }

            if (ocrPageCount > 0) {
                log.info("Used OCR for {} of {} pages", ocrPageCount, totalPages);
            }
            log.info("Extracted text from {} pages (total {})", pages.size(), totalPages);
            return pages;
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to extract text from PDF", e);
        }
    }

    /**
     * OCR a single page by rendering it to an image and running Tesseract.
     */
    private String ocrSinglePage(PDFRenderer renderer, int pageIndex) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, OCR_DPI, ImageType.GRAY);
            Tesseract tesseract = createTesseract();
            String result = tesseract.doOCR(image);
            log.debug("OCR page {} extracted {} characters", pageIndex + 1, result.length());
            return result;
        } catch (IOException | TesseractException e) {
            log.warn("OCR failed for page {}: {}", pageIndex + 1, e.getMessage());
            return null;
        }
    }

    /**
     * OCR the entire document (used when the whole PDF is scanned).
     */
    private String ocrEntireDocument(PDDocument document) {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            String pageText = ocrSinglePage(renderer, i);
            if (pageText != null && !pageText.isBlank()) {
                sb.append(pageText).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Create a configured Tesseract instance.
     */
    private Tesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // LSTM neural net mode
        return tesseract;
    }

    /**
     * Get total number of pages in PDF.
     */
    public int getPageCount(InputStream pdfInputStream) {
        try (PDDocument document = Loader.loadPDF(pdfInputStream.readAllBytes())) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read PDF", e);
        }
    }

    /**
     * Chunk text by headings first, then by size limits.
     * Returns chunks with associated page numbers.
     */
    public List<TextChunk> chunkByHeading(List<PageContent> pages) {
        List<TextChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentStartPage = 1;
        int chunkIndex = 0;

        for (PageContent page : pages) {
            String[] lines = page.text().split("\n");

            for (String line : lines) {
                Matcher headingMatcher = HEADING_PATTERN.matcher(line.trim());

                // If we hit a heading and have accumulated content, save the chunk
                if (headingMatcher.find() && !currentChunk.isEmpty()) {
                    List<TextChunk> splitChunks = splitLargeChunk(
                            currentChunk.toString(), currentStartPage, chunkIndex);
                    chunks.addAll(splitChunks);
                    chunkIndex += splitChunks.size();

                    currentChunk = new StringBuilder();
                    currentStartPage = page.pageNumber();
                }

                currentChunk.append(line).append("\n");
            }
        }

        // Don't forget the last chunk
        if (!currentChunk.isEmpty()) {
            List<TextChunk> splitChunks = splitLargeChunk(
                    currentChunk.toString(), currentStartPage, chunkIndex);
            chunks.addAll(splitChunks);
        }

        log.info("Created {} chunks from {} pages", chunks.size(), pages.size());
        return chunks;
    }

    /**
     * Split a large chunk into smaller ones with overlap.
     */
    private List<TextChunk> splitLargeChunk(String text, int pageNumber, int startIndex) {
        List<TextChunk> chunks = new ArrayList<>();
        String trimmed = text.trim();

        if (trimmed.length() <= MAX_CHUNK_SIZE) {
            if (trimmed.length() >= MIN_CHUNK_SIZE) {
                chunks.add(new TextChunk(trimmed, pageNumber, startIndex));
            }
            return chunks;
        }

        int start = 0;
        int index = startIndex;
        while (start < trimmed.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, trimmed.length());

            // Try to break at sentence boundary
            if (end < trimmed.length()) {
                int sentenceEnd = trimmed.lastIndexOf(". ", end);
                if (sentenceEnd > start + MIN_CHUNK_SIZE) {
                    end = sentenceEnd + 1;
                }
            }

            String chunk = trimmed.substring(start, end).trim();
            if (chunk.length() >= MIN_CHUNK_SIZE) {
                chunks.add(new TextChunk(chunk, pageNumber, index++));
            }

            // If we reached the end of the text, we're done
            if (end >= trimmed.length()) {
                break;
            }

            // Move forward with overlap, but always make progress
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }

        return chunks;
    }

    public record PageContent(int pageNumber, String text) {}
    public record TextChunk(String content, int pageNumber, int chunkIndex) {}
}
