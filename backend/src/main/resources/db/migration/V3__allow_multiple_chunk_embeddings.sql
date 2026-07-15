ALTER TABLE chunk_embedding
    DROP CONSTRAINT chunk_embedding_document_chunk_id_key;

ALTER TABLE chunk_embedding
    ADD CONSTRAINT chunk_embedding_document_chunk_model_id_key
        UNIQUE (document_chunk_id, embedding_model_id);
