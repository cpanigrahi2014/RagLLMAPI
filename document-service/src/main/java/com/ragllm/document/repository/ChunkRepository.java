package com.ragllm.document.repository;

import com.ragllm.common.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    List<Chunk> findAllByChapterId(UUID chapterId);

    List<Chunk> findAllByTenantId(UUID tenantId);

    @Modifying
    @Query(value = "DELETE FROM chunks WHERE chapter_id = :chapterId", nativeQuery = true)
    void deleteAllByChapterId(@Param("chapterId") UUID chapterId);

    @Modifying
    @Query(value = "DELETE FROM chunks WHERE chapter_id IN (SELECT id FROM chapters WHERE book_id = :bookId)", nativeQuery = true)
    void deleteAllByBookId(@Param("bookId") UUID bookId);

    long countByTenantId(UUID tenantId);

    /**
     * Vector similarity search using pgvector.
     * Returns chunks for a specific tenant ordered by cosine distance.
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
     * Vector similarity search with distance score.
     */
    @Query(value = """
            SELECT c.id, c.tenant_id, c.chapter_id, c.content, c.embedding,
                   c.page_number, c.chunk_index, c.created_at,
                   (1 - (c.embedding <-> cast(:queryVector as vector))) as similarity_score
            FROM chunks c
            WHERE c.tenant_id = :tenantId
            ORDER BY c.embedding <-> cast(:queryVector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarChunksWithScore(
            @Param("tenantId") UUID tenantId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit);
}
