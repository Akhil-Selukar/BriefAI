package com.briefai.document.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.document.dto.DocumentResponse;
import com.briefai.document.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @RequestPart("file") MultipartFile file) {
        return documentService.uploadDocument(currentUser.getId(), file);
    }

    @GetMapping
    public List<DocumentResponse> getDocuments(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return documentService.getDocuments(currentUser.getId());
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long documentId) {
        return documentService.getDocument(currentUser.getId(), documentId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long documentId) {
        documentService.deleteDocument(currentUser.getId(), documentId);
    }
}
