package com.briefai.document.parser;

import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.exception.document.DocumentParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class WordDocumentParserTest {
    private WordDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new WordDocumentParser();
    }

    @Test
    void supports_shouldAcceptDocAndDocx() {
        assertTrue(parser.supports("application/msword"));
        assertTrue(parser.supports("application/vnd.openxmlformats-officedocument" + ".wordprocessingml.document"));

        assertFalse(parser.supports("application/pdf"));
        assertFalse(parser.supports("image/png"));
    }

    @Test
    void parse_shouldExtractDocxText() throws Exception {
        assertExtractedText("documents/test-doc.docx");
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

            assertEquals(1, result.getPages().size());
            assertNull(result.getPages().get(0).getPageNumber());
            assertNull(result.getPageCount());
            assertTrue(result.getPages().get(0).getText().startsWith("Video provides a powerful"));
        }
    }
}