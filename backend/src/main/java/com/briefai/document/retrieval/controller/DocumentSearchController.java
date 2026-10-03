package com.briefai.document.retrieval.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.document.chunks.service.DocumentChunkService;
import com.briefai.document.retrieval.dto.DocumentSearchRequest;
import com.briefai.document.retrieval.dto.RetrievedChunk;
import com.briefai.document.retrieval.service.DocumentRetrievalService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents/search")
public class DocumentSearchController {
    private static final Logger logger = LoggerFactory.getLogger(DocumentChunkService.class);
    private final DocumentRetrievalService retrievalService;

    public DocumentSearchController(DocumentRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping
    public List<RetrievedChunk> search(@AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody DocumentSearchRequest request) {
        logger.debug("Request received to search chunks based on question asked");
        return retrievalService.search(currentUser.getId(), request.getQuestion());
    }
}