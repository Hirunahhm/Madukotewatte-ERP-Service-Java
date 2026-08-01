-- ============================================================
-- V12__latex_tap_rate_and_attendance_transaction.sql
-- Lets attendance trees-tapped auto-generate a Latex_Tap earning:
-- each employee gets a configurable rate per tree, and attendance
-- rows can link to the employee_transaction they generated (mirrors
-- labour's existing transaction_record_id link) so re-editing an
-- already-paid attendance record doesn't double-count earnings.
-- ============================================================

ALTER TABLE employees ADD COLUMN rate_per_tree DECIMAL(10, 2) NOT NULL DEFAULT 0;

ALTER TABLE attendance ADD COLUMN transaction_record_id VARCHAR(36)
    REFERENCES employee_transactions(transaction_record_id);
