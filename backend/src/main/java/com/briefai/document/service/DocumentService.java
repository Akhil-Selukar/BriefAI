package com.briefai.document.service;

import com.briefai.document.dto.DocumentResponse;
import com.briefai.document.entity.Document;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.document.DocumentNotFoundException;
import com.briefai.exception.storage.*;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.storage.service.FileStorageService;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private final long maxFileSizeBytes;
    private final long maxDocumentsPerUser;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, FileStorageService fileStorageService,
                           @Value("${app.document.max-file-size-bytes}") long maxFileSizeBytes,
                           @Value("${app.document.max-documents-per-user}") long maxDocumentsPerUser) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxDocumentsPerUser = maxDocumentsPerUser;
    }

    public DocumentResponse uploadDocument(Long userId, MultipartFile file) {
        logger.debug("Uploading the document");
        validateFile(userId, file);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.error("User with id {} trying to upload the document does not exist", userId);
            throw new UserNotFoundException("user not found.");
        }

        String storageKey = null;

        try {
            storageKey = fileStorageService.store(file.getInputStream(), file.getOriginalFilename(), userId);

            Document document = new Document(user, file.getOriginalFilename(), storageKey, file.getContentType(), file.getSize());
            Document saved = documentRepository.save(document);

            return toResponse(saved);

        } catch (IOException e) {
            logger.error("Unable to read the uploaded file.");
            throw new FileStorageException("Could not read uploaded file.", e);
        } catch (RuntimeException e) {
            // In case of document is successfully stored but it's metadata is not stored in DB then the document will be
            // orphan, so to avoid this we need to delete the document.
            if (storageKey != null) {
                try {
                    fileStorageService.delete(storageKey);
                } catch (RuntimeException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw e;
        }
    }

    public List<DocumentResponse> getDocuments(Long userId) {
        logger.debug("Fetching documents for user with id {}", userId);
        return documentRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DocumentResponse getDocument(Long userId, Long documentId) {
        logger.debug("Fetching document with id {} for user with id {}", documentId, userId);
        Document document = documentRepository.findByIdAndUserId(documentId, userId).orElse(null);

        if(document == null) {
            logger.warn("Document not found");
            throw new DocumentNotFoundException("Document not found");
        }

        return toResponse(document);
    }

    public void deleteDocument(Long userId, Long documentId) {
        logger.debug("Deleting the document with if {}", documentId);
        Document document = documentRepository.findByIdAndUserId(documentId, userId).orElse(null);

        if(document == null) {
            logger.warn("Document not found");
            throw new DocumentNotFoundException("Document not found");
        }

        fileStorageService.delete(document.getStorageKey());

        documentRepository.delete(document);
    }

    private void validateFile(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("Uploaded document is empty.");
            throw new EmptyDocumentException("Document must not be empty.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            logger.warn("Uploaded document has size greater than 10 mb");
            throw new DocumentTooLargeException("Document must not exceed 10 mb.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            logger.warn("Uploaded document file type is not supported.");
            throw new UnsupportedDocumentTypeException("Only PDF, DOC, and DOCX documents are supported.");
        }

        long existingDocuments = documentRepository.countByUserId(userId);

        if (existingDocuments >= maxDocumentsPerUser) {
            logger.warn("Maximum document per user limit reached");
            throw new DocumentLimitExceededException("Maximum document limit reached.");
        }
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getOriginalName(), document.getContentType(), document.getSizeBytes(),
                document.getPageCount(), document.getStatus(), document.getCreatedAt());
    }
}
