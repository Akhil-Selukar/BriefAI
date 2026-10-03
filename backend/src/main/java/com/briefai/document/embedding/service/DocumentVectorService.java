package com.briefai.document.embedding.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import com.briefai.document.embedding.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentVectorService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentEmbeddingService.class);
    private final DocumentEmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    public DocumentVectorService(DocumentEmbeddingService embeddingService, DocumentChunkRepository chunkRepository) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    public void store(List<DocumentChunk> chunks) {
        logger.debug("Embedding and storing the document chunks");
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty list of chunks.");
        }

        List<EmbeddedChunk> embedded = embeddingService.embed(chunks);
        chunkRepository.saveAll(embedded);
    }
}
