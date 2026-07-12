CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE source_document (
    id UUID PRIMARY KEY,
    resource_path TEXT NOT NULL UNIQUE,
    content_hash TEXT NOT NULL,
    indexed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE document_chunk (
    id UUID PRIMARY KEY,
    source_document_id UUID NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    chunking_strategy_version TEXT NOT NULL,
    CONSTRAINT document_chunk_unique_index UNIQUE (source_document_id, chunk_index),
    CONSTRAINT document_chunk_offsets_check CHECK (
        start_offset >= 0
        AND end_offset > start_offset
    )
);

CREATE TABLE chunk_embedding (
    id UUID PRIMARY KEY,
    document_chunk_id UUID NOT NULL UNIQUE REFERENCES document_chunk(id) ON DELETE CASCADE,
    embedding VECTOR(384) NOT NULL,
    embedding_model_id TEXT NOT NULL,
    embedding_model_version TEXT NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    chunking_strategy_version TEXT NOT NULL,
    embedded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chunk_embedding_dimensions_check CHECK (
        embedding_dimensions = vector_dims(embedding)
    )
);

CREATE INDEX document_chunk_source_document_id_idx
    ON document_chunk(source_document_id);
