package com.briefai.document.parser;

import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.exception.storage.document.DocumentParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentParserTest {
    private PdfDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new PdfDocumentParser();
    }

    @Test
    void supports_shouldAcceptDocAndDocx() {
        assertTrue(parser.supports("application/pdf"));

        assertFalse(parser.supports("application/msword"));
        assertFalse(parser.supports("application/vnd.openxmlformats-officedocument" + ".wordprocessingml.document"));
        assertFalse(parser.supports("image/png"));
    }

    @Test
    void parse_shouldExtractDocxText() throws Exception {
        assertExtractedText("documents/test-pdf.pdf");
    }

    @Test
    void parse_shouldRejectInvalidWordDocument() {
        InputStream invalidFile = new java.io.ByteArrayInputStream(new byte[]{0, 1, 2, 3});

        ParsedDocument result = null;

        try {
            result = parser.parse(invalidFile);
        } catch (DocumentParsingException e) {
            return;
        }

        assertTrue(result.getPages().stream().allMatch(page -> page.getText().isBlank()));
    }

    private void assertExtractedText(String resource) throws Exception {

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {

            assertNotNull(input, "Missing fixture: " + resource);

            ParsedDocument result = parser.parse(input);

            assertEquals(8, result.getPages().size());
            assertEquals(1, result.getPages().get(0).getPageNumber());
            assertEquals(8, result.getPageCount());
            assertTrue(result.getPages().get(0).getText().startsWith("Video provides a powerful"));
        }
    }
}