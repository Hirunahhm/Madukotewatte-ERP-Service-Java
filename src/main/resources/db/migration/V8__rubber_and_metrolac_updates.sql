-- ============================================================
-- V8__rubber_and_metrolac_updates.sql
-- Adds a proper collection timestamp to rubber_solid_records
-- (previously only had inherited created_at), restores the
-- metrolac reading value that the frontend form captured but
-- never persisted, and aligns the ammonia_records type values
-- with what the frontend actually sends ('refill'/'usage')
-- instead of the original 'Refill'/'Out'.
-- ============================================================

ALTER TABLE rubber_solid_records ADD COLUMN timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE metrolac_readings ADD COLUMN reading DECIMAL(6, 2);

ALTER TABLE ammonia_records DROP CONSTRAINT ammonia_records_type_check;
UPDATE ammonia_records SET type = 'refill' WHERE type = 'Refill';
UPDATE ammonia_records SET type = 'usage' WHERE type = 'Out';
ALTER TABLE ammonia_records ADD CONSTRAINT ammonia_records_type_check
    CHECK (type IN ('refill', 'usage'));
