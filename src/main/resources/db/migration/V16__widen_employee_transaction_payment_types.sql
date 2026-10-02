-- Salary payments should settle against real bank accounts (matching Cash & Debt's
-- ASSET_TYPES), not the coarse cash/bank_transfer domain shared with labour payouts.
ALTER TABLE employee_transactions DROP CONSTRAINT employee_transactions_payment_type_check;
ALTER TABLE employee_transactions ADD CONSTRAINT employee_transactions_payment_type_check
    CHECK (payment_type IS NULL OR payment_type IN ('bank_transfer', 'cash', 'Cash', 'Bank-BOC', 'Bank-Seylan', 'Bank-Peoples'));
