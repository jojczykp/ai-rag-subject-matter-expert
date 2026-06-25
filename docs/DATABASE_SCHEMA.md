# Database Schema

## Purpose

This document describes the application database schema. Keep it aligned with
Flyway migrations under `src/main/resources/db/migration`.

## Current Schema

The initial schema stores bundled source document metadata, deterministic chunks,
and one embedding per chunk for the statically configured embedding model.

```mermaid
erDiagram
    SOURCE_DOCUMENT ||--o{ DOCUMENT_CHUNK : contains
    DOCUMENT_CHUNK ||--o| CHUNK_EMBEDDING : has

    SOURCE_DOCUMENT {
        uuid id PK
        text resource_path UK
        text content_hash
        timestamptz indexed_at
    }

    DOCUMENT_CHUNK {
        uuid id PK
        uuid source_document_id FK
        int chunk_index
        text content
        int start_offset
        int end_offset
        text chunking_strategy_version
    }

    CHUNK_EMBEDDING {
        uuid id PK
        uuid document_chunk_id FK, UK
        vector embedding
        text embedding_model_id
        text embedding_model_version
        int embedding_dimensions
        text chunking_strategy_version
        timestamptz embedded_at
    }
```
