package com.briefai.document.parser;

import com.briefai.document.parser.dto.ParsedDocument;

import java.io.InputStream;

public interface DocumentParser {
    boolean supports(String contentType);
    ParsedDocument parse(InputStream inputStream);
}
