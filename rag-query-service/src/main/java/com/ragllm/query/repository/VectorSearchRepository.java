package com.ragllm.query.repository;

import com.ragllm.common.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VectorSearchRepository extends JpaRepository<Chunk, UUID> {

    /**
     * Core vector similarity search with tenant isolation.
     * Uses pgvector's <-> operator for cosine distance.
     */
    @Query(value = """
            SELECT c.id, c.tenant_id, c.chapter_id, c.content, c.embedding,
                   c.page_number, c.chunk_index, c.created_at
            FROM chunks c
            WHERE c.tenant_id = :tenantId
            ORDER BY c.embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Chunk> findSimilarChunks(
            @Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit);

    /**
     * Vector similarity search with distance score returned.
     */
    @Query(value = """
            SELECT c.id as id,
                   c.content as content,
                   c.page_number as pageNumber,
                   c.chunk_index as chunkIndex,
                   c.chapter_id as chapterId,
                   (1 - (c.embedding <-> cast(:queryVector as vector))) as similarity
            FROM chunks c
            WHERE c.tenant_id = :tenantId
            ORDER BY c.embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksWithScore(
            @Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit);

    /**
     * Vector similarity search filtered by subject.
     */
    @Query(value = """
            SELECT c.id as id,
                   c.content as content,
                   c.page_number as pageNumber,
                   c.chunk_index as chunkIndex,
                   c.chapter_id as chapterId,
                   (1 - (c.embedding <-> cast(:queryVector as vector))) as similarity
            FROM chunks c
            JOIN chapters ch ON c.chapter_id = ch.id
            JOIN books b ON ch.book_id = b.id
            WHERE c.tenant_id = :tenantId
              AND LOWER(b.subject) = LOWER(:subject)
            ORDER BY c.embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksBySubject(
            @Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit,
            @Param("subject") String subject);

    /**
     * Vector similarity search filtered by class level.
     */
    @Query(value = """
            SELECT c.id as id,
                   c.content as content,
                   c.page_number as pageNumber,
                   c.chunk_index as chunkIndex,
                   c.chapter_id as chapterId,
                   (1 - (c.embedding <-> cast(:queryVector as vector))) as similarity
            FROM chunks c
            JOIN chapters ch ON c.chapter_id = ch.id
            JOIN books b ON ch.book_id = b.id
            WHERE c.tenant_id = :tenantId
              AND b.class_level = :classLevel
            ORDER BY c.embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksByClass(
            @Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit,
            @Param("classLevel") Integer classLevel);

    /**
     * Vector similarity search filtered by subject AND class level.
     */
    @Query(value = """
            SELECT c.id as id,
                   c.content as content,
                   c.page_number as pageNumber,
                   c.chunk_index as chunkIndex,
                   c.chapter_id as chapterId,
                   (1 - (c.embedding <-> cast(:queryVector as vector))) as similarity
            FROM chunks c
            JOIN chapters ch ON c.chapter_id = ch.id
            JOIN books b ON ch.book_id = b.id
            WHERE c.tenant_id = :tenantId
              AND LOWER(b.subject) = LOWER(:subject)
              AND b.class_level = :classLevel
            ORDER BY c.embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksBySubjectAndClass(
            @Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit,
            @Param("subject") String subject,
            @Param("classLevel") Integer classLevel);

    /**
     * Fetch chunk text content for a specific book (for summarization/QA generation).
     * Ordered by chunk_index so we get sequential content.
     */
    @Query(value = """
            SELECT c.content, c.page_number, c.chunk_index
            FROM chunks c
            JOIN chapters ch ON c.chapter_id = ch.id
            WHERE ch.book_id = :bookId
              AND c.tenant_id = :tenantId
            ORDER BY c.chunk_index ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findChunkContentByBookId(
            @Param("bookId") UUID bookId,
            @Param("tenantId") UUID tenantId,
            @Param("limit") int limit);
}
