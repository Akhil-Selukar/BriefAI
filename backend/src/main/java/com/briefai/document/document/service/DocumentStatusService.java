package com.briefai.document.document.service;

import com.briefai.document.document.entity.Document;
import com.briefai.document.document.entity.DocumentStatus;
import com.briefai.document.document.repository.DocumentRepository;
import com.briefai.exception.document.DocumentNotFoundException;
import com.briefai.exception.document.IllegalDocumentStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DocumentStatusService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentStatusService.class);
    private final DocumentRepository documentRepository;

    public DocumentStatusService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startProcessing(Long documentId) {
        int updated = documentRepository.claimForProcessing(documentId, DocumentStatus.UPLOADED,
                DocumentStatus.PROCESSING, LocalDateTime.now());

        if (updated != 1) {
            logger.warn("Document is not available for processing");
            throw new IllegalDocumentStateException("Document is not available for processing.");
        }
        logger.debug("Document processed.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException("Document not found."));

        document.markFailed();
    }
}
