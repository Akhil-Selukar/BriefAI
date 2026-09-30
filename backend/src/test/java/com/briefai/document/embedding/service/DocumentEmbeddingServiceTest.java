package com.briefai.document.embedding.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private DocumentEmbeddingService service;

    @Test
    void embed_shouldGenerateVectorForEachChunk() {
        DocumentChunk chunk = new DocumentChunk(100L, 1L, 0, 1, "Test document text");
        float[] vector = new float[768];
        vector[0] = 0.25f;

        when(embeddingModel.embed(chunk.getText())).thenReturn(vector);

        List<EmbeddedChunk> result = service.embed(List.of(chunk));

        assertEquals(1, result.size());
        assertEquals(chunk, result.get(0).getChunk());
        assertArrayEquals(vector, result.get(0).getEmbedding());
    }


    @Test
    void embed_shouldRejectWrongDimensions() {
        DocumentChunk chunk = new DocumentChunk(100L, 1L, 0, 1, "Test document text");
        float[] vector = new float[780];

        when(embeddingModel.embed(chunk.getText())).thenReturn(vector);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> service.embed(List.of(chunk)));
        assertEquals("Embedding model returned an invalid vector.", thrown.getMessage());
    }

    @Test
    void embed_shouldRejectNonFiniteValues() {
        DocumentChunk chunk = new DocumentChunk(100L, 1L, 0, 1, "Test document text");
        float[] vector = new float[768];
        vector[0] = Float.POSITIVE_INFINITY;

        when(embeddingModel.embed(chunk.getText())).thenReturn(vector);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> service.embed(List.of(chunk)));
        assertEquals("Embedding contains a infinite value.", thrown.getMessage());

    }
}