package com.briefai.document.retrieval.service;

import com.briefai.document.embedding.repository.DocumentChunkRepository;
import com.briefai.document.retrieval.dto.RetrievedChunk;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentRetrievalService {
    private final EmbeddingModel embeddingModel;
    private final DocumentChunkRepository chunkRepository;

    public DocumentRetrievalService(EmbeddingModel embeddingModel, DocumentChunkRepository chunkRepository) {
        this.embeddingModel = embeddingModel;
        this.chunkRepository = chunkRepository;
    }

    public List<RetrievedChunk> search(Long userId, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty.");
        }

        float[] embedding = embeddingModel.embed(question.trim());
        return chunkRepository.search(userId, embedding, 5);
    }
}
