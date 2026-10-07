package com.briefai.document.document.dto;

import com.briefai.document.document.entity.DocumentStatus;

import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private String originalName;
    private String contentType;
    private long sizeBytes;
    private Integer pageCount;
    private DocumentStatus status;
    private LocalDateTime createdAt;

    public DocumentResponse(Long id, String originalName, String contentType, long sizeBytes, Integer pageCount,
                            DocumentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.pageCount = pageCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
