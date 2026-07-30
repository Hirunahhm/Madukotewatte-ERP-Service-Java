-- ============================================================
-- V5__expand_attendance_no_work.sql
-- Expand attendance.no_work to match all frontend absence reasons
-- ============================================================

-- Widen column first: 'public_holiday' (14 chars) and 'family_matter' (13 chars) exceed VARCHAR(10)
ALTER TABLE attendance ALTER COLUMN no_work TYPE VARCHAR(20);

ALTER TABLE attendance DROP CONSTRAINT IF EXISTS attendance_no_work_check;

-- Remap existing values to their new equivalent names
UPDATE attendance SET no_work = 'sick' WHERE no_work = 'ill';
UPDATE attendance SET no_work = 'public_holiday' WHERE no_work = 'holiday';
UPDATE attendance SET no_work = 'other' WHERE no_work = 'no_loads';

ALTER TABLE attendance ADD CONSTRAINT attendance_no_work_check
    CHECK (no_work IN ('none', 'rain', 'sick', 'other', 'public_holiday', 'funeral', 'family_matter', 'kids'));
