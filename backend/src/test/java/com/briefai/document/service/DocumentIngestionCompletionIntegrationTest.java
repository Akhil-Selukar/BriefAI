package com.briefai.document.service;

import com.briefai.document.chunks.dto.DocumentChunk;
import com.briefai.document.document.service.DocumentIngestionCompletionService;
import com.briefai.document.embedding.dto.EmbeddedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_OLLAMA_INTEGRATION_TESTS", matches = "true")
class DocumentIngestionCompletionIntegrationTest {

    @Autowired
    private DocumentIngestionCompletionService completionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void complete_shouldRollbackChunksWhenSecondInsertFails() {

        Long userId = null;

        try {
            String email = "test-user" + UUID.randomUUID() + "@example.com";

            userId = jdbcTemplate.queryForObject(
                    """
                            INSERT INTO users (
                                name, email, password, email_verified,
                                created_at, updated_at
                            )
                            VALUES (
                                'Rollback Test User', ?,
                                'test-password-not-used', true,
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                            )
                            RETURNING id
                            """,
                    Long.class,
                    email
            );

            assertNotNull(userId);

            Long documentId = jdbcTemplate.queryForObject(
                    """
                            INSERT INTO documents (
                                user_id, original_name, storage_key,
                                content_type, size_bytes, status,
                                created_at, updated_at
                            )
                            VALUES (
                                ?, 'rollback-test.pdf', ?,
                                'application/pdf', 1024, 'PROCESSING',
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                            )
                            RETURNING id
                            """,
                    Long.class,
                    userId,
                    userId + "/" + UUID.randomUUID() + ".pdf"
            );

            assertNotNull(documentId);

            DocumentChunk first = new DocumentChunk(documentId, userId, 0, 1, "First chunk");
            DocumentChunk second = new DocumentChunk(documentId, userId, 0, 1, "Second chunk");
            float[] vector = new float[768];
            vector[0] = 0.25f;

            List<EmbeddedChunk> embedded = List.of(new EmbeddedChunk(first, vector), new EmbeddedChunk(second, vector));

            assertThrows(RuntimeException.class, () -> completionService.complete(documentId, embedded, 1));

            Integer chunkCount = jdbcTemplate.queryForObject(
                    """
                            SELECT count(*)
                            FROM document_chunks
                            WHERE document_id = ?
                            """,
                    Integer.class,
                    documentId
            );

            assertEquals(0, chunkCount);

            String status = jdbcTemplate.queryForObject(
                    """
                            SELECT status
                            FROM documents
                            WHERE id = ?
                            """,
                    String.class,
                    documentId
            );

            assertEquals("PROCESSING", status);

        } finally {
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
            }
        }
    }
}