package com.briefai.document.embedding.repository;

import com.briefai.document.embedding.dto.EmbeddedChunk;
import com.briefai.document.retrieval.dto.RetrievedChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class DocumentChunkRepository {
    private final JdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveAll(List<EmbeddedChunk> chunks) {

        String sql = """
                INSERT INTO document_chunks (
                    document_id,
                    user_id,
                    chunk_index,
                    page_number,
                    content,
                    embedding
                )
                VALUES (?, ?, ?, ?, ?, ?::vector)
                """;

        for (EmbeddedChunk embedded : chunks) {
            var chunk = embedded.getChunk();

            jdbcTemplate.update(sql,
                    chunk.getDocumentId(),
                    chunk.getUserId(),
                    chunk.getChunkIndex(),
                    chunk.getPageNumber(),
                    chunk.getText(),
                    toVectorLiteral(embedded.getEmbedding())
            );
        }
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(vector[i]);
        }

        return result.append("]").toString();
    }

    public List<RetrievedChunk> search(Long userId, float[] queryEmbedding, int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Search limit must be between 1 and 20.");
        }

        String sql = """
                SELECT
                    dc.document_id,
                    d.original_name,
                    dc.chunk_index,
                    dc.page_number,
                    dc.content,
                    1 - (dc.embedding <=> ?::vector) AS similarity
                FROM document_chunks dc
                JOIN documents d
                    ON d.id = dc.document_id
                WHERE dc.user_id = ?
                  AND d.user_id = ?
                  AND d.status = 'READY'
                ORDER BY dc.embedding <=> ?::vector
                LIMIT ?
                """;

        String vector = toVectorLiteral(queryEmbedding);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RetrievedChunk(
                        rs.getLong("document_id"),
                        rs.getString("original_name"),
                        rs.getInt("chunk_index"),
                        (Integer) rs.getObject("page_number"),
                        rs.getString("content"),
                        rs.getDouble("similarity")
                ),
                vector,
                userId,
                userId,
                vector,
                limit
        );
    }
}
