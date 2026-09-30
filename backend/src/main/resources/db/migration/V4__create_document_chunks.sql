CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    page_number INTEGER,
    content TEXT NOT NULL,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chunks_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_chunks_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_document_chunk_index
        UNIQUE (document_id, chunk_index),

    CONSTRAINT valid_chunk_index
        CHECK (chunk_index >= 0),

    CONSTRAINT valid_chunk_page
        CHECK (page_number IS NULL OR page_number > 0),

    CONSTRAINT valid_chunk_content
        CHECK (length(trim(content)) > 0)
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);

CREATE INDEX idx_document_chunks_user_id ON document_chunks(user_id);