package com.briefai.document.retrieval.service;

import com.briefai.document.embedding.repository.DocumentChunkRepository;
import com.briefai.document.retrieval.dto.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DATABASE_INTEGRATION_TESTS", matches = "true")
class DocumentRetrievalIntegrationTest {

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void search_shouldReturnOnlyOwnedReadyDocuments() {

        Long userA = createUser();
        Long userB = createUser();

        Long relevantDocument = createDocument(userA, "relevant.pdf", "READY");
        Long lessRelevantDocument = createDocument(userA, "less-relevant.pdf", "READY");
        Long foreignDocument = createDocument(userB, "private.pdf", "READY");
        Long uploadedDocument = createDocument(userA, "unprocessed.pdf", "UPLOADED");

        float[] relevantVector = vector(1.0f, 0.0f);
        float[] lessRelevantVector = vector(0.6f, 0.8f);

        insertChunk(relevantDocument, userA, "Relevant RAG information", relevantVector);
        insertChunk(lessRelevantDocument, userA, "Less relevant information", lessRelevantVector);

        // An exact match owned by another user.
        insertChunk(foreignDocument, userB, "Another user's private information", relevantVector);

        // An exact match that is not READY.
        insertChunk(uploadedDocument, userA, "Unprocessed information", relevantVector);

        List<RetrievedChunk> results = chunkRepository.search(userA, vector(1.0f, 0.0f), 5);

        assertEquals(2, results.size());
        assertEquals(relevantDocument, results.get(0).getDocumentId());
        assertEquals(lessRelevantDocument, results.get(1).getDocumentId());
        assertEquals("relevant.pdf", results.get(0).getDocumentName());
        assertEquals(1.0, results.get(0).getSimilarity(), 0.0001);
        assertTrue(results.get(0).getSimilarity() > results.get(1).getSimilarity());
        assertTrue(results.stream().noneMatch(chunk -> chunk.getDocumentId().equals(foreignDocument) || chunk.getDocumentId().equals(uploadedDocument)));

        // Verify the opposite direction of user isolation.
        List<RetrievedChunk> userBResults = chunkRepository.search(userB, vector(1.0f, 0.0f), 5);

        assertEquals(1, userBResults.size());
        assertEquals(foreignDocument, userBResults.get(0).getDocumentId());
    }

    private Long createUser() {

        String email = "test-user" + UUID.randomUUID() + "@example.com";

        return jdbcTemplate.queryForObject(
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
                            'Retrieval Test',
                            ?,
                            'unused-test-password',
                            true,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                Long.class,
                email
        );
    }

    private Long createDocument(Long userId, String name, String status) {

        String storageKey = userId + "/" + UUID.randomUUID() + ".pdf";

        return jdbcTemplate.queryForObject(
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
                            ?, ?, ?, 'application/pdf',
                            1024, ?,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                Long.class,
                userId,
                name,
                storageKey,
                status
        );
    }

    private void insertChunk(Long documentId, Long userId, String content, float[] embedding) {

        jdbcTemplate.update(
                """
                        INSERT INTO document_chunks (
                            document_id,
                            user_id,
                            chunk_index,
                            page_number,
                            content,
                            embedding
                        )
                        VALUES (?, ?, 0, 1, ?, ?::vector)
                        """,
                documentId,
                userId,
                content,
                toVectorLiteral(embedding)
        );
    }

    private float[] vector(float first, float second) {

        float[] embedding = new float[768];

        embedding[0] = first;
        embedding[1] = second;

        return embedding;
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(embedding[i]);
        }
        return result.append("]").toString();
    }
}