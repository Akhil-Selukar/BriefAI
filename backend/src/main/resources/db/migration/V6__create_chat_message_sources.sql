CREATE TABLE chat_message_sources (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    document_id BIGINT,
    source_number INTEGER NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    page_number INTEGER,
    chunk_index INTEGER NOT NULL,
    similarity DOUBLE PRECISION NOT NULL,

    CONSTRAINT fk_chat_message_source_message
        FOREIGN KEY (message_id)
        REFERENCES chat_messages(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_chat_message_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE SET NULL,

    CONSTRAINT valid_chat_message_source_number
        CHECK (source_number > 0),

    CONSTRAINT valid_chat_message_source_page
        CHECK (page_number IS NULL OR page_number > 0),

    CONSTRAINT valid_chat_message_source_chunk
        CHECK (chunk_index >= 0),

    CONSTRAINT unique_chat_message_source_number
        UNIQUE (message_id, source_number)
);

CREATE INDEX idx_chat_message_sources_message_id ON chat_message_sources(message_id);

CREATE INDEX idx_chat_message_sources_document_id ON chat_message_sources(document_id);