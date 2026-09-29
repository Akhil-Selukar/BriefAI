package com.briefai.document.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.chunks.service.DocumentChunkService;
import com.briefai.document.entity.Document;
import com.briefai.document.parser.DocumentParser;
import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.storage.document.DocumentNotFoundException;
import com.briefai.exception.storage.document.DocumentParsingException;
import com.briefai.storage.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final List<DocumentParser> parsers;
    private final DocumentStatusService statusService;
    private final DocumentChunkService documentChunkService;

    public DocumentIngestionService(DocumentRepository documentRepository, FileStorageService fileStorageService,
                                    List<DocumentParser> parsers, DocumentStatusService statusService, DocumentChunkService documentChunkService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.parsers = parsers;
        this.statusService = statusService;
        this.documentChunkService = documentChunkService;
    }

    public ParsedDocument extract(Long documentId) {
        statusService.startProcessing(documentId);
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException("Document not found."));

            DocumentParser parser = parsers.stream().filter(p -> p.supports(document.getContentType())).findFirst()
                    .orElseThrow(() -> new DocumentParsingException("No parser available for document type."));

            ParsedDocument result;

            InputStream inputStream = fileStorageService.open(document.getStorageKey());
            result = parser.parse(inputStream);
            validate(result);

            return result;

        } catch (RuntimeException ex) {
            try {
                statusService.markFailed(documentId);
            } catch (RuntimeException statusException) {
                ex.addSuppressed(statusException);
            }

            throw ex;
        }
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

    public List<DocumentChunk> extractAndChunk(Long documentId, Long userId) {
        ParsedDocument parsed = extract(documentId);

        return documentChunkService.chunk(documentId, userId, parsed);
    }
}
