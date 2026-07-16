ALTER TABLE source_document
    ADD COLUMN subject_id TEXT;

UPDATE source_document
SET subject_id = 'culinary-expert'
WHERE subject_id IS NULL;

ALTER TABLE source_document
    ALTER COLUMN subject_id SET NOT NULL;

ALTER TABLE source_document
    DROP CONSTRAINT source_document_resource_path_key;

ALTER TABLE source_document
    ADD CONSTRAINT source_document_subject_resource_path_key
        UNIQUE (subject_id, resource_path);

CREATE INDEX source_document_subject_id_idx
    ON source_document(subject_id);
