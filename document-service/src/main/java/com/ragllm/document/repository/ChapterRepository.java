package com.ragllm.document.repository;

import com.ragllm.common.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findAllByBookId(UUID bookId);

    @Modifying
    @Query(value = "DELETE FROM chapters WHERE book_id = :bookId", nativeQuery = true)
    void deleteAllByBookId(@Param("bookId") UUID bookId);
}
