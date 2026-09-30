package com.briefai.document.service;

import com.briefai.document.embedding.dto.EmbeddedChunk;
import com.briefai.document.embedding.repository.DocumentChunkRepository;
import com.briefai.document.entity.DocumentStatus;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.document.DocumentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentIngestionCompletionService {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public DocumentIngestionCompletionService(DocumentChunkRepository documentChunkRepository, DocumentRepository documentRepository) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public void complete(Long documentId, List<EmbeddedChunk> chunks, Integer pageCount) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Cannot complete ingestion without chunks.");
        }

        var document = documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException("Document not found."));

        if (document.getStatus() != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("Document is not being processed.");
        }

        boolean invalidOwnership = chunks.stream()
                .anyMatch(embedded -> !documentId.equals(embedded.getChunk().getDocumentId())
                        || !document.getUser().getId().equals(embedded.getChunk().getUserId()));

        if (invalidOwnership) {
            throw new IllegalArgumentException("Chunk ownership does not match document.");
        }

        documentChunkRepository.saveAll(chunks);
        document.markReady(pageCount);
    }
}
