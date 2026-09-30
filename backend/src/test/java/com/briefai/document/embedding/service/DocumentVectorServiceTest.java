package com.briefai.document.embedding.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import com.briefai.document.embedding.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentVectorServiceTest {
    @Mock
    private DocumentEmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @InjectMocks
    private DocumentVectorService vectorService;

    private DocumentChunk chunk;

    @BeforeEach
    void setUp() {
        chunk = new DocumentChunk(100L, 1L, 0, 1, "Test document content");
    }

    @Test
    void store_shouldPersistGeneratedEmbeddings() {
        List<DocumentChunk> chunks = List.of(chunk);

        float[] vector = new float[768];
        vector[0] = 0.25f;

        List<EmbeddedChunk> embeddedChunks = List.of(new EmbeddedChunk(chunk, vector));

        when(embeddingService.embed(chunks)).thenReturn(embeddedChunks);

        vectorService.store(chunks);

        // In order ensures tha the embeddingService call succeed first then chunkRepository call.
        InOrder inOrder = inOrder(embeddingService, chunkRepository);

        inOrder.verify(embeddingService).embed(chunks);
        inOrder.verify(chunkRepository).saveAll(embeddedChunks);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void store_shouldNotPersistWhenEmbeddingFails() {
        List<DocumentChunk> chunks = List.of(chunk);

        RuntimeException embeddingFailure = new RuntimeException("Ollama unavailable");

        when(embeddingService.embed(chunks)).thenThrow(embeddingFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> vectorService.store(chunks));
        assertSame(embeddingFailure, thrown);
        verify(embeddingService).embed(chunks);
        verifyNoInteractions(chunkRepository);
    }

    @Test
    void store_shouldRejectEmptyChunks() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> vectorService.store(List.of()));

        assertEquals("Cannot store an empty list of chunks.", thrown.getMessage());
        verifyNoInteractions(embeddingService, chunkRepository);
    }
}