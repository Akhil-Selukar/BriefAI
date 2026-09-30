package com.briefai.document.retrieval.service;

import com.briefai.document.embedding.repository.DocumentChunkRepository;
import com.briefai.document.retrieval.dto.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentRetrievalServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @InjectMocks
    private DocumentRetrievalService retrievalService;

    private float[] queryEmbedding;

    @BeforeEach
    void setUp() {
        queryEmbedding = new float[768];
        queryEmbedding[0] = 1.0f;
    }

    @Test
    void search_shouldEmbedQuestionAndReturnChunks() {
        String question = "What is the ?";
        RetrievedChunk chunk = new RetrievedChunk(100L, "research.pdf", 0, 1,
                "RAG is a technique of generate better AI model response by searching outside the models knowledge base.", 0.95);

        List<RetrievedChunk> expected = List.of(chunk);

        when(embeddingModel.embed(question)).thenReturn(queryEmbedding);
        when(chunkRepository.search(1L, queryEmbedding, 5)).thenReturn(expected);

        List<RetrievedChunk> actual = retrievalService.search(1L, question);

        assertEquals(expected, actual);
        assertEquals(1, actual.size());
        assertEquals(100L, actual.get(0).getDocumentId());
        assertEquals(0.95, actual.get(0).getSimilarity());

        // Confirm that embedding happens before retrieval.
        var order = inOrder(embeddingModel, chunkRepository);

        order.verify(embeddingModel).embed(question);
        order.verify(chunkRepository).search(1L, queryEmbedding, 5);
        order.verifyNoMoreInteractions();
    }

    @Test
    void search_shouldRejectBlankOrNullQuestion() {
        assertThrows(IllegalArgumentException.class, () -> retrievalService.search(1L, "  "));
        assertThrows(IllegalArgumentException.class, () -> retrievalService.search(1L, null));
        verifyNoInteractions(embeddingModel, chunkRepository);
    }

    @Test
    void search_shouldPropagateEmbeddingFailure() {
        String question = "What is RAG?";
        RuntimeException failure = new RuntimeException("Ollama unavailable");
        when(embeddingModel.embed(question)).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> retrievalService.search(1L, question));

        assertEquals(failure.getMessage(), thrown.getMessage());
        verify(embeddingModel).embed(question);
        verifyNoInteractions(chunkRepository);
    }
}