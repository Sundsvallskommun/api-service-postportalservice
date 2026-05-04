ALTER TABLE recipient
    ADD COLUMN party_type VARCHAR(20) NULL AFTER party_id;

UPDATE recipient SET party_type = 'PRIVATE' WHERE party_type IS NULL;
