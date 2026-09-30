package com.briefai.document.embedding.repository;

import com.briefai.document.embedding.dto.EmbeddedChunk;
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
}
