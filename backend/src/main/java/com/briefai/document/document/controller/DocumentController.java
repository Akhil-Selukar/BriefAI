package com.briefai.document.document.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.document.document.dto.DocumentResponse;
import com.briefai.document.document.service.DocumentIngestionService;
import com.briefai.document.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final DocumentIngestionService ingestionService;

    public DocumentController(DocumentService documentService, DocumentIngestionService ingestionService) {
        this.documentService = documentService;
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @RequestPart("file") MultipartFile file) {
        logger.debug("Request received to upload a document");
        return documentService.uploadDocument(currentUser.getId(), file);
    }

    @GetMapping
    public List<DocumentResponse> getDocuments(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        logger.debug("Request received to get list of documents uploaded by the user");
        return documentService.getDocuments(currentUser.getId());
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long documentId) {
        logger.debug("Request received to get document based on document id");
        return documentService.getDocument(currentUser.getId(), documentId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long documentId) {
        logger.debug("Request received to delete document with id {}", documentId);
        documentService.deleteDocument(currentUser.getId(), documentId);
    }

    @PostMapping("/{documentId}/process")
    public DocumentResponse processDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long documentId) {
        // Reject non existing documents and documents owned by another user before starting ingestion.
        // this getDocument call throws document not found in case of document is not owned by current user or non existing document id
        logger.debug("Request received to start processing document with id {}", documentId);
        documentService.getDocument(currentUser.getId(), documentId);

        ingestionService.process(documentId);

        // Fetch the updated metadata after ingestion completes.
        return documentService.getDocument(currentUser.getId(), documentId);
    }
}
