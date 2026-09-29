package com.briefai.document.parser;

import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.parser.dto.ParsedPage;
import com.briefai.exception.storage.document.DocumentParsingException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class WordDocumentParser implements DocumentParser{

    private static final String DOC_TYPE = "application/msword";
    private static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public boolean supports(String contentType) {
        return contentType.equals(DOC_TYPE) || contentType.equals(DOCX_TYPE);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);    // -1 removes Tika's default character limit.
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            String text = handler.toString().trim();

            return new ParsedDocument(List.of(new ParsedPage(null, text)), null);
        } catch (Exception e) {
            throw new DocumentParsingException("Could not parse Word document.", e);
        }
    }
}
