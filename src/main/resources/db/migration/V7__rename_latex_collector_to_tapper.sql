-- ============================================================
-- V7__rename_latex_collector_to_tapper.sql
-- Standardize employee position: "Latex Collector" -> "Tapper"
-- ============================================================

UPDATE employees SET position = 'Tapper' WHERE position = 'Latex Collector';
