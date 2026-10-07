package com.briefai.document.embedding;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.embedding.service.DocumentVectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_OLLAMA_INTEGRATION_TESTS", matches = "true")
class DocumentVectorIntegrationTest {

    @Autowired
    private DocumentVectorService vectorService;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void store_shouldGenerateAndPersistRealEmbedding() {

        // Create an isolated user for this test.
        String email = "test_user" + UUID.randomUUID() + "@example.com";

        Long userId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO users (
                            name,
                            email,
                            password,
                            email_verified,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            'Vector Test User',
                             ?,
                            'test-password-not-used',
                             true,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                Long.class,
                email
        );

        assertNotNull(userId);

        // Create an UPLOADED document belonging to this user.
        Long documentId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO documents (
                            user_id,
                            original_name,
                            storage_key,
                            content_type,
                            size_bytes,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            ?,
                            'integration-test.pdf',
                            ?,
                            'application/pdf',
                            1024,
                            'UPLOADED',
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                Long.class,
                userId,
                userId + "/" + UUID.randomUUID() + ".pdf"
        );

        assertNotNull(documentId);

        DocumentChunk chunk = new DocumentChunk(documentId, userId, 0, 1,
                "Test document content to answer questions from user based on this document.");

        // Real call: Spring AI -> Ollama -> PostgreSQL.
        vectorService.store(List.of(chunk));

        // Read the row back from PostgreSQL.
        StoredChunk stored = jdbcTemplate.queryForObject(
                """
                        SELECT
                            document_id,
                            user_id,
                            chunk_index,
                            page_number,
                            content,
                            vector_dims(embedding) AS dimensions
                        FROM document_chunks
                        WHERE document_id = ?
                        """,
                (rs, rowNum) -> new StoredChunk(
                        rs.getLong("document_id"),
                        rs.getLong("user_id"),
                        rs.getInt("chunk_index"),
                        rs.getInt("page_number"),
                        rs.getString("content"),
                        rs.getInt("dimensions")
                ),
                documentId
        );

        assertNotNull(stored);
        assertEquals(documentId, stored.getDocumentId());
        assertEquals(userId, stored.getUserId());
        assertEquals(0, stored.getChunkIndex());
        assertEquals(1, stored.getPageNumber());
        assertEquals(chunk.getText(), stored.getContent());
        assertEquals(768, stored.getDimensions());

        // Also verify the real model's configured dimensions.
        assertEquals(768, embeddingModel.dimensions());
    }

    private class StoredChunk {
        private Long documentId;
        private Long userId;
        private int chunkIndex;
        private int pageNumber;
        private String content;
        private int dimensions;

        public StoredChunk(Long documentId, Long userId, int chunkIndex, int pageNumber, String content, int dimensions) {
            this.documentId = documentId;
            this.userId = userId;
            this.chunkIndex = chunkIndex;
            this.pageNumber = pageNumber;
            this.content = content;
            this.dimensions = dimensions;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public void setChunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }
}