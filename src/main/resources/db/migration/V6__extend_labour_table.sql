-- ============================================================
-- V6__extend_labour_table.sql
-- Add business timestamp + payment type to the (previously orphan) labour table
-- ============================================================

ALTER TABLE labour ADD COLUMN timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE labour ADD COLUMN payment_type VARCHAR(20)
    CHECK (payment_type IN ('bank_transfer', 'cash'));
