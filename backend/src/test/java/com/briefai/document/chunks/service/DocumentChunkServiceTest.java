package com.briefai.document.chunks.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.parser.dto.ParsedPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkServiceTest {
    private final DocumentChunkService service = new DocumentChunkService(5, 2);

    @Test
    void chunk_shouldReturnOneChunkForShortText() {
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(1, "This is the content of test document")), 1);

        List<DocumentChunk> chunks = service.chunk(100L, 1L, parsed);

        assertEquals(2, chunks.size());
        assertEquals(100L, chunks.get(0).getDocumentId());
        assertEquals(1L, chunks.get(0).getUserId());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(0).getPageNumber());
        assertEquals("This is the content of", chunks.get(0).getText());
    }

    @Test
    void chunk_shouldCreateOverlappingChunks() {
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(1, "This is the content of test document")), 1);

        List<DocumentChunk> chunks = service.chunk(100L, 1L, parsed);

        assertEquals(2, chunks.size());
        assertEquals("This is the content of", chunks.get(0).getText());
        assertEquals("content of test document", chunks.get(1).getText());
    }

    @Test
    void chunk_shouldPreservePdfPageNumbers() {

        ParsedDocument parsed = new ParsedDocument(
                List.of(new ParsedPage(1, "First page content"),
                        new ParsedPage(2, "Second page content")
                ),
                2
        );

        List<DocumentChunk> chunks = service.chunk(100L, 1L, parsed);

        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).getPageNumber());
        assertEquals(2, chunks.get(1).getPageNumber());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(1).getChunkIndex());
    }

    @Test
    void chunk_shouldSkipEmptyPages() {

        ParsedDocument parsed = new ParsedDocument(
                List.of(
                        new ParsedPage(1, "   "),
                        new ParsedPage(2, "Content on second page content"),
                        new ParsedPage(3, "     ")
                ),
                3
        );

        List<DocumentChunk> chunks = service.chunk(100L, 1L, parsed);

        assertEquals(1, chunks.size());
        assertEquals(2, chunks.get(0).getPageNumber());
    }

    @Test
    void chunk_shouldSupportWordWithoutPageNumber() {
        ParsedDocument parsed = new ParsedDocument(List.of(new ParsedPage(null, "Test document content")), null);

        List<DocumentChunk> chunks = service.chunk(100L, 1L, parsed);

        assertEquals(1, chunks.size());
        assertNull(chunks.get(0).getPageNumber());
    }

    @Test
    void constructor_shouldRejectInvalidConfiguration() {

        assertThrows(IllegalArgumentException.class, () -> new DocumentChunkService(5, 5));
        assertThrows(IllegalArgumentException.class, () -> new DocumentChunkService(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DocumentChunkService(5, -1));
    }
}