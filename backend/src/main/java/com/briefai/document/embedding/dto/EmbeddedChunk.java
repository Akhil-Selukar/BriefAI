package com.briefai.document.embedding.dto;

import com.briefai.document.chunks.dto.DocumentChunk;

public class EmbeddedChunk {
    private DocumentChunk chunk;
    private float[] embedding;

    public EmbeddedChunk(DocumentChunk chunk, float[] embedding) {
        this.chunk = chunk;
        this.embedding = embedding;
    }

    public DocumentChunk getChunk() {
        return chunk;
    }

    public void setChunk(DocumentChunk chunk) {
        this.chunk = chunk;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
