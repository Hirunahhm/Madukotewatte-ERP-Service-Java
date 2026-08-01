-- ============================================================
-- V14__employee_role_and_transaction_cleanup.sql
-- Drops the per-crop harvest piece-rates added in V13 along with the
-- auto-generated Banana/Coconut/Manioc_Harvest payroll transactions —
-- crop production tracking stays, only the employee-rate-driven
-- auto-pay mechanism goes. Adds Salary_Payment so a salary payout can
-- finally be recorded against an employee.
-- ============================================================

ALTER TABLE employees DROP COLUMN rate_per_bunch;
ALTER TABLE employees DROP COLUMN rate_per_nut;
ALTER TABLE employees DROP COLUMN rate_per_kg_manioc;

ALTER TABLE banana_records DROP COLUMN transaction_record_id;
ALTER TABLE coconut_records DROP COLUMN transaction_record_id;
ALTER TABLE manioc_records DROP COLUMN transaction_record_id;

ALTER TABLE employee_transactions DROP CONSTRAINT employee_transactions_type_check;
ALTER TABLE employee_transactions ADD CONSTRAINT employee_transactions_type_check
    CHECK (type IN ('Manual_Labor', 'Advance', 'Loan_Payment', 'Latex_Tap', 'Salary_Payment'));
