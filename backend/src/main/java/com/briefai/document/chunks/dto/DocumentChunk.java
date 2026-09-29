package com.briefai.document.chunks.dto;

public class DocumentChunk {
    private Long documentId;
    private Long userId;
    private int chunkIndex;
    private Integer pageNumber;
    private String text;

    public DocumentChunk(Long documentId, Long userId, int chunkIndex, Integer pageNumber, String text) {
        this.documentId = documentId;
        this.userId = userId;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.text = text;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
