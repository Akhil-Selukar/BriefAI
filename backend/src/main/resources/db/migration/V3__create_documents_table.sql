CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    page_count INTEGER,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT unique_documents_storage_key
        UNIQUE (storage_key),
    CONSTRAINT documents_size_constraint
        CHECK (size_bytes >= 0),
    CONSTRAINT documents_page_count_constraint
        CHECK (page_count IS NULL OR page_count >= 0)
);

CREATE INDEX idx_documents_user_id ON documents(user_id);