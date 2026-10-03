package com.briefai.document.parser;

import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.parser.dto.ParsedPage;
import com.briefai.exception.document.DocumentParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentParser implements DocumentParser{
    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentParser.class);
    private static final String PDF_TYPE = "application/pdf";

    @Override
    public boolean supports(String contentType) {
        return contentType.equals(PDF_TYPE);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream) {
        logger.debug("Parsing the pdf document.");
        try {
            byte[] bytes = inputStream.readAllBytes();

            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                int pageCount = pdf.getNumberOfPages();

                if (pageCount > 100) {
                    logger.error("Document exceed the allowed page limit of 100 pages");
                    throw new DocumentParsingException("Document exceeds the 100-page limit.");
                }

                PDFTextStripper stripper = new PDFTextStripper();
                List<ParsedPage> pages = new ArrayList<>();

                for (int page = 1; page <= pageCount; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);

                    String text = stripper.getText(pdf).trim();

                    pages.add(new ParsedPage(page, text));
                }

                return new ParsedDocument(List.copyOf(pages), pageCount);
            }

        } catch (IOException e) {
            throw new DocumentParsingException("Could not parse PDF document.", e);
        }
    }

}
