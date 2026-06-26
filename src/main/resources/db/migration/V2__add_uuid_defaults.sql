ALTER TABLE source_document
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE document_chunk
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE chunk_embedding
    ALTER COLUMN id SET DEFAULT gen_random_uuid();
