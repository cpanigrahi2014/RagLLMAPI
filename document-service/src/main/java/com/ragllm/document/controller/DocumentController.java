package com.ragllm.document.controller;

import com.ragllm.common.dto.ApiResponse;
import com.ragllm.common.dto.BookUploadResponse;
import com.ragllm.common.entity.Book;
import com.ragllm.document.service.DocumentIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final DocumentIngestionService documentIngestionService;

    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    /**
     * Upload a document for processing.
     * Supports PDF, DOC, DOCX, and TXT files.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<BookUploadResponse>> uploadBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String bookName,
            @RequestParam("subject") String subject,
            @RequestParam("classLevel") Integer classLevel,
            @RequestParam(value = "embeddingModel", required = false) String embeddingModel) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is required"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unsupported file type. Accepted: PDF, DOC, DOCX, TXT"));
        }

        BookUploadResponse response = documentIngestionService.uploadBook(file, bookName, subject, classLevel, embeddingModel);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Book upload initiated", response));
    }

    /**
     * Delete a book and all associated chapters, chunks, and embeddings.
     */
    @DeleteMapping("/books/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteBook(@PathVariable UUID bookId) {
        documentIngestionService.deleteBook(bookId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("bookId", bookId.toString(), "status", "DELETED")));
    }

    /**
     * List all books for current tenant.
     */
    @GetMapping("/books")
    public ResponseEntity<ApiResponse<List<Book>>> getBooks() {
        List<Book> books = documentIngestionService.getBooks();
        return ResponseEntity.ok(ApiResponse.success(books));
    }

    /**
     * Get book details by ID.
     */
    @GetMapping("/books/{bookId}")
    public ResponseEntity<ApiResponse<Book>> getBook(@PathVariable UUID bookId) {
        Book book = documentIngestionService.getBookById(bookId);
        return ResponseEntity.ok(ApiResponse.success(book));
    }
}
