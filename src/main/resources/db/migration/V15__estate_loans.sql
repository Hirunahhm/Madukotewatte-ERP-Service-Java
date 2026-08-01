-- ============================================================
-- V15__estate_loans.sql
-- Estate debt has had zero per-loan identity: 'Loan-mom' and
-- 'Loan-other' were just two fixed catch-all buckets, so two
-- different personal loans collapsed into one running balance with
-- no way to tell them apart. This introduces a real estate_loans
-- table (id, name, issued date, principal, balance) to replace those
-- two buckets going forward. Credit cards already have workable
-- identity (2 named cards + credit_card_limits + statement view) and
-- are untouched here.
--
-- No production data to preserve (dev/test only) — the 2 retired
-- buckets' historical ledger rows are dropped outright rather than
-- migrated.
-- ============================================================

CREATE TABLE estate_loans (
    loan_id             VARCHAR(36)     PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    principal_amount    DECIMAL(15, 2)  NOT NULL,
    current_balance     DECIMAL(15, 2)  NOT NULL,
    issued_date         DATE            NOT NULL,
    interest_rate       DECIMAL(5, 2),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

DELETE FROM estate_loan_transactions WHERE loan_type IN ('Loan-mom', 'Loan-other');

ALTER TABLE estate_loan_transactions ALTER COLUMN loan_type DROP NOT NULL;
ALTER TABLE estate_loan_transactions DROP CONSTRAINT estate_loan_transactions_loan_type_check;
ALTER TABLE estate_loan_transactions ADD CONSTRAINT estate_loan_transactions_loan_type_check
    CHECK (loan_type IS NULL OR loan_type IN ('credit-card - Peoples', 'credit-card - Sampath'));
ALTER TABLE estate_loan_transactions ADD COLUMN loan_id VARCHAR(36) REFERENCES estate_loans(loan_id);
ALTER TABLE estate_loan_transactions ADD CONSTRAINT estate_loan_transactions_type_or_loan_check
    CHECK ((loan_type IS NOT NULL) <> (loan_id IS NOT NULL));
