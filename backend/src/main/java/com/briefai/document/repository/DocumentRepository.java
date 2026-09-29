package com.briefai.document.repository;

import com.briefai.document.entity.Document;
import com.briefai.document.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Document> findByIdAndUserId(Long documentId, Long userId);
    long countByUserId(Long userId);

    @Modifying
    @Query("""
    UPDATE Document d
       SET d.status = :processing,
           d.updatedAt = :updatedAt
       WHERE d.id = :documentId AND d.status = :uploaded
    """)
    int claimForProcessing(@Param("documentId") Long documentId, @Param("uploaded") DocumentStatus uploaded,
                           @Param("processing") DocumentStatus processing, @Param("updatedAt") LocalDateTime updatedAt);
}
