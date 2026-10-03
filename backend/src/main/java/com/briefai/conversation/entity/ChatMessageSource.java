package com.briefai.conversation.entity;

import com.briefai.document.entity.Document;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_message_sources", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_chat_message_source_number",
                columnNames = {"message_id", "source_number"}
        )
}
)
public class ChatMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "source_number", nullable = false)
    private int sourceNumber;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "similarity", nullable = false)
    private double similarity;

    protected ChatMessageSource() {
    }

    public ChatMessageSource(ChatMessage message, Document document, int sourceNumber, String documentName, Integer pageNumber, int chunkIndex, double similarity) {
        if (message == null) {
            throw new IllegalArgumentException("Message is required.");
        }

        if (sourceNumber <= 0) {
            throw new IllegalArgumentException("Source number must be positive.");
        }

        if (documentName == null || documentName.isBlank()) {
            throw new IllegalArgumentException("Document name is required.");
        }

        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative.");
        }

        this.message = message;
        this.document = document;
        this.sourceNumber = sourceNumber;
        this.documentName = documentName;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.similarity = similarity;
    }

    public Long getId() {
        return id;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public Document getDocument() {
        return document;
    }

    public int getSourceNumber() {
        return sourceNumber;
    }

    public String getDocumentName() {
        return documentName;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public double getSimilarity() {
        return similarity;
    }
}
