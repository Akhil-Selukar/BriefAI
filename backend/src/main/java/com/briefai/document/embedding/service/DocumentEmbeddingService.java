package com.briefai.document.embedding.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentEmbeddingService {
    private static final int EMBEDDING_DIMENSIONS = 768;
    private final EmbeddingModel embeddingModel;

    public DocumentEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<EmbeddedChunk> embed(List<DocumentChunk> chunks) {
        List<EmbeddedChunk> results = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            float[] vector = embeddingModel.embed(chunk.getText());

            if (vector == null || vector.length != EMBEDDING_DIMENSIONS) {
                throw new IllegalStateException("Embedding model returned an invalid vector.");
            }

            for (float value : vector) {
                if (!Float.isFinite(value)) {
                    throw new IllegalStateException("Embedding contains a infinite value.");
                }
            }

            results.add(new EmbeddedChunk(chunk, vector));
        }

        return List.copyOf(results);
    }
}
