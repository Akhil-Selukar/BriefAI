package com.briefai.document.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.chunks.service.DocumentChunkService;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import com.briefai.document.embedding.service.DocumentEmbeddingService;
import com.briefai.document.entity.Document;
import com.briefai.document.parser.DocumentParser;
import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.document.DocumentNotFoundException;
import com.briefai.exception.document.DocumentParsingException;
import com.briefai.storage.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class DocumentIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final List<DocumentParser> parsers;
    private final DocumentStatusService statusService;
    private final DocumentChunkService documentChunkService;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final DocumentIngestionCompletionService completionService;

    public DocumentIngestionService(DocumentRepository documentRepository, FileStorageService fileStorageService,
                                    List<DocumentParser> parsers, DocumentStatusService statusService,
                                    DocumentChunkService documentChunkService, DocumentEmbeddingService documentEmbeddingService,
                                    DocumentIngestionCompletionService completionService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.parsers = parsers;
        this.statusService = statusService;
        this.documentChunkService = documentChunkService;
        this.documentEmbeddingService = documentEmbeddingService;
        this.completionService = completionService;
    }

    private void validate(ParsedDocument document) {

        if (document == null || document.getPages() == null || document.getPages().isEmpty()) {
            throw new DocumentParsingException("Document contains no extractable text.");
        }

        boolean hasText = document.getPages().stream().anyMatch(page -> page != null
                && page.getText() != null && !page.getText().isBlank());

        if (!hasText) {
            throw new DocumentParsingException("Document contains no extractable text.");
        }

        if (document.getPageCount() != null && document.getPageCount() > 100) {
            throw new DocumentParsingException("Document exceeds the 100-page limit.");
        }
    }

    public void process(Long documentId) {
        logger.debug("Starting to process document with id {}", documentId);
        // If another worker has already processing the document then we should not touch it
        statusService.startProcessing(documentId);

        try {
            Document document = documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException("Document not found."));
            DocumentParser parser = parsers.stream().filter(p -> p.supports(document.getContentType())).findFirst()
                    .orElseThrow(() -> new DocumentParsingException("No parser available."));

            ParsedDocument parsed;
            try (InputStream input = fileStorageService.open(document.getStorageKey())) {
                parsed = parser.parse(input);
            } catch (IOException e) {
                throw new DocumentParsingException("Could not read document.", e);
            }

            validate(parsed);

            List<DocumentChunk> chunks = documentChunkService.chunk(document.getId(), document.getUser().getId(), parsed);
            if (chunks.isEmpty()) {
                throw new DocumentParsingException("Document produced no usable chunks.");
            }

            List<EmbeddedChunk> embedded = documentEmbeddingService.embed(chunks);
            completionService.complete(documentId, embedded, parsed.getPageCount());

        } catch (RuntimeException e) {
            try {
                logger.error("Document processing failed.");
                statusService.markFailed(documentId);
            } catch (RuntimeException statusException) {
                e.addSuppressed(statusException);
            }
            throw e;
        }
    }
}
