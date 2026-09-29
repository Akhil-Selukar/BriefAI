package com.briefai.document.chunks.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.parser.dto.ParsedDocument;
import com.briefai.document.parser.dto.ParsedPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkService {
    private final int chunkSize;
    private final int overlap;

    public DocumentChunkService(@Value("${app.document.chunk-size-words:200}") int chunkSize, @Value("${app.document.chunk-overlap-words:40}") int overlap) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk size must be positive and overlap must be smaller than chunk size.");
        }

        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<DocumentChunk> chunk(Long documentId, Long userId, ParsedDocument parsedDocument) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for(ParsedPage page : parsedDocument.getPages()) {  // for all pages in the document
            if (page == null || page.getText() == null || page.getText().isBlank()) {
                continue;
            }

            String[] words = page.getText().trim().split("\\s+");       // get all words on the page

            // create chunks
            int start = 0;
            while (start < words.length) {
                int end = Math.min(start + chunkSize, words.length);

                String content = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));
                chunks.add(new DocumentChunk(documentId, userId, chunks.size(), page.getPageNumber(), content));

                if (end == words.length) {
                    break;
                }

                start = end - overlap;      // adjust the overlaping words
            }
        }

        return List.copyOf(chunks);
    }
}
