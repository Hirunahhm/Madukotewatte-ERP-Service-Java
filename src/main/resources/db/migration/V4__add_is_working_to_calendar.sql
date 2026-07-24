-- ============================================================
-- V4__add_is_working_to_calendar.sql
-- Madukotawatte Estate ERP - Add is_working to calendar
-- ============================================================

ALTER TABLE calendar ADD COLUMN is_working BOOLEAN NOT NULL DEFAULT TRUE;
