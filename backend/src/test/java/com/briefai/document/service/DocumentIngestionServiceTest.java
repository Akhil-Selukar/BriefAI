package com.briefai.document.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.chunks.service.DocumentChunkService;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import com.briefai.document.embedding.service.DocumentEmbeddingService;
import com.briefai.document.entity.Document;
import com.briefai.document.parser.DocumentParser;
import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.parser.dto.ParsedPage;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.document.DocumentParsingException;
import com.briefai.storage.service.FileStorageService;
import com.briefai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentParser pdfParser;

    @Mock
    private DocumentStatusService statusService;

    @Mock
    private DocumentChunkService chunkingService;

    @Mock
    private DocumentEmbeddingService embeddingService;

    @Mock
    private DocumentIngestionCompletionService completionService;

    private DocumentIngestionService ingestionService;

    private Document document;
    private ParsedDocument parsed;
    private List<DocumentChunk> chunks;
    private List<EmbeddedChunk> embedded;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService(documentRepository, fileStorageService, List.of(pdfParser),
                statusService, chunkingService, embeddingService, completionService);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);

        document = new Document(user, "research.pdf", "1/test.pdf", "application/pdf", 1024L);
        ReflectionTestUtils.setField(document, "id", 100L);

        parsed = new ParsedDocument(
                List.of(
                        new ParsedPage(1, "This is 1st page"),
                        new ParsedPage(2, "This is second page")
                ),
                2
        );

        chunks = List.of(new DocumentChunk(100L, 1L, 0, 1, "Test document chunk one"),
                new DocumentChunk(100L, 1L, 1, 2, "Test document chunk two"));

        embedded = List.of(
                new EmbeddedChunk(chunks.get(0), new float[768]),
                new EmbeddedChunk(chunks.get(1), new float[768])
        );
    }

    private void stubSuccessfulExtraction() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(pdfParser.supports("application/pdf")).thenReturn(true);
        when(fileStorageService.open("1/test.pdf")).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(pdfParser.parse(any(InputStream.class))).thenReturn(parsed);
    }

    @Test
    void process_shouldCompleteSuccessfulIngestion() {
        stubSuccessfulExtraction();

        when(chunkingService.chunk(100L, 1L, parsed)).thenReturn(chunks);
        when(embeddingService.embed(chunks)).thenReturn(embedded);

        ingestionService.process(100L);

        InOrder order = inOrder(statusService, fileStorageService, pdfParser, chunkingService, embeddingService, completionService);

        order.verify(statusService).startProcessing(100L);
        order.verify(fileStorageService).open("1/test.pdf");
        order.verify(pdfParser).parse(any(InputStream.class));
        order.verify(chunkingService).chunk(100L, 1L, parsed);
        order.verify(embeddingService).embed(chunks);
        order.verify(completionService).complete(100L, embedded, 2);

        verify(statusService, never()).markFailed(anyLong());
    }

    @Test
    void process_shouldMarkFailedWhenParsingFails() {

        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(pdfParser.supports("application/pdf")).thenReturn(true);
        when(fileStorageService.open("1/test.pdf")).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        DocumentParsingException failure = new DocumentParsingException("Invalid PDF");
        when(pdfParser.parse(any(InputStream.class))).thenThrow(failure);
        DocumentParsingException thrown = assertThrows(DocumentParsingException.class, () -> ingestionService.process(100L));

        assertSame(failure, thrown);

        verify(statusService).startProcessing(100L);
        verify(statusService).markFailed(100L);
        verifyNoInteractions(chunkingService, embeddingService, completionService);
    }

    @Test
    void process_shouldMarkFailedWhenEmbeddingFails() {
        stubSuccessfulExtraction();
        when(chunkingService.chunk(100L, 1L, parsed)).thenReturn(chunks);

        RuntimeException failure = new RuntimeException("Ollama unavailable");
        when(embeddingService.embed(chunks)).thenThrow(failure);

        assertThrows(RuntimeException.class, () -> ingestionService.process(100L));
        verify(statusService).markFailed(100L);
        verifyNoInteractions(completionService);
    }

    @Test
    void process_shouldMarkFailedWhenCompletionFails() {
        stubSuccessfulExtraction();
        when(chunkingService.chunk(100L, 1L, parsed)).thenReturn(chunks);
        when(embeddingService.embed(chunks)).thenReturn(embedded);
        RuntimeException failure = new RuntimeException("Database insert failed");

        doThrow(failure).when(completionService).complete(100L, embedded, 2);

        assertThrows(RuntimeException.class, () -> ingestionService.process(100L));
        verify(statusService).markFailed(100L);
    }

    @Test
    void process_shouldNotMarkFailedWhenClaimFails() {

        IllegalStateException failure = new IllegalStateException("Document is not available for processing.");

        doThrow(failure).when(statusService).startProcessing(100L);

        assertThrows(IllegalStateException.class, () -> ingestionService.process(100L));
        verify(statusService, never()).markFailed(anyLong());
        verifyNoInteractions(documentRepository, fileStorageService, pdfParser, chunkingService, embeddingService, completionService);
    }
}