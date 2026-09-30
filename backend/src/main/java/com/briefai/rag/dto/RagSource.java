package com.briefai.rag.dto;

public class RagSource {
    private int sourceNumber;
    private Long documentId;
    private String documentName;
    private Integer pageNumber;
    private int chunkIndex;
    private double similarity;

    public RagSource(int sourceNumber, Long documentId, String documentName, Integer pageNumber, int chunkIndex, double similarity) {
        this.sourceNumber = sourceNumber;
        this.documentId = documentId;
        this.documentName = documentName;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.similarity = similarity;
    }

    public int getSourceNumber() {
        return sourceNumber;
    }

    public void setSourceNumber(int sourceNumber) {
        this.sourceNumber = sourceNumber;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
