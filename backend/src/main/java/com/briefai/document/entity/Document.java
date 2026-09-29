package com.briefai.document.entity;

import com.briefai.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Document() {
    }

    public Document(User user, String originalName, String storageKey, String contentType, long sizeBytes) {
        this.user = user;
        this.originalName = originalName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = DocumentStatus.UPLOADED;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        if (status != DocumentStatus.UPLOADED) {
            throw new IllegalStateException("Only uploaded documents can be processed.");
        }

        status = DocumentStatus.PROCESSING;
        updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        if (status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("Only processing documents can fail.");
        }

        status = DocumentStatus.FAILED;
        updatedAt = LocalDateTime.now();
    }

    public void markReady(int pageCount) {
        if (status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("Only processing documents can be ready.");
        }

        this.pageCount = pageCount;
        status = DocumentStatus.READY;
        updatedAt = LocalDateTime.now();
    }
}
