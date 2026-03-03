package com.ragllm.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chunks", indexes = {
    @Index(name = "idx_chunks_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_chunks_chapter_id", columnList = "chapter_id")
})
@Filter(name = "tenantFilter", condition = "tenant_id = cast(:tenantId as uuid)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JsonIgnore
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    private Chapter chapter;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
