-- ============================================================
-- V21__fixed_assets_and_borrowings.sql  (Wave 2c)
-- Fixed assets: depreciation parameters, purchase/disposal ledger links.
-- Estate loans: lender/term/instalment/notes, dated movements, interest split, ledger links.
-- ============================================================

-- ── 1. Fixed assets ─────────────────────────────────────────
ALTER TABLE fixed_assets
    ADD COLUMN useful_life_years        INTEGER,
    ADD COLUMN residual_value           DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN depreciation_method      VARCHAR(20)   NOT NULL DEFAULT 'STRAIGHT_LINE',
    ADD COLUMN purchase_account_code    VARCHAR(40) REFERENCES financial_accounts(code),
    ADD COLUMN purchase_transaction_id  VARCHAR(36),
    ADD COLUMN disposal_date            DATE,
    ADD COLUMN disposal_proceeds        DECIMAL(15,2),
    ADD COLUMN disposal_account_code    VARCHAR(40) REFERENCES financial_accounts(code),
    ADD COLUMN disposal_transaction_id  VARCHAR(36);

ALTER TABLE fixed_assets ADD CONSTRAINT fixed_assets_method_check
    CHECK (depreciation_method IN ('STRAIGHT_LINE', 'NONE'));

-- Default useful lives per category (owner can edit per asset):
--   land -> not depreciated; building 20y; vehicle 5y; machinery 10y; equipment / tools 5y;
--   plantation / mature rubber / trees 25y; anything else 5y.
UPDATE fixed_assets SET depreciation_method = 'NONE', useful_life_years = NULL
 WHERE lower(category) = 'land';
UPDATE fixed_assets SET useful_life_years = CASE
        WHEN lower(category) = 'building' THEN 20
        WHEN lower(category) = 'vehicle' THEN 5
        WHEN lower(category) = 'machinery' THEN 10
        WHEN lower(category) IN ('equipment', 'tools') THEN 5
        WHEN lower(category) IN ('plantation', 'mature rubber', 'trees') THEN 25
        ELSE 5 END
 WHERE lower(category) <> 'land';

-- current_value is kept in sync by the service (= NBV today at last write); the API computes it live.

-- Disposed assets recorded before this migration: stamp a disposal date, keep NBV as it was.
UPDATE fixed_assets SET disposal_date = CAST(updated_at AS DATE), disposal_proceeds = 0
 WHERE status = 'disposed' AND disposal_date IS NULL;

-- ── 2. Estate loans ─────────────────────────────────────────
ALTER TABLE estate_loans
    ADD COLUMN lender              VARCHAR(150),
    ADD COLUMN term_months         INTEGER,
    ADD COLUMN installment_amount  DECIMAL(15,2),
    ADD COLUMN notes               TEXT;

ALTER TABLE estate_loan_transactions
    ADD COLUMN seq                    BIGSERIAL,
    ADD COLUMN transaction_date       DATE,
    ADD COLUMN amount                 DECIMAL(15,2),
    ADD COLUMN interest_amount        DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN ledger_transaction_id  VARCHAR(36);

UPDATE estate_loan_transactions
   SET transaction_date = CAST(created_at AS DATE),
       amount = ABS(COALESCE(new_amount, 0) - COALESCE(last_amount, 0));

ALTER TABLE estate_loan_transactions ALTER COLUMN transaction_date SET NOT NULL;
ALTER TABLE estate_loan_transactions ALTER COLUMN amount SET NOT NULL;

-- Link existing repayment rows to their principal ledger row (best effort: same loan, same amount, in order)
UPDATE estate_loan_transactions t
   SET ledger_transaction_id = m.id
  FROM monetary_asset_transactions m
 WHERE t.transaction_type = 'repay'
   AND m.reference_type = 'ESTATE_LOAN' AND m.reference_id = t.loan_id
   AND m.category = 'ESTATE_LOAN_REPAYMENT' AND m.amount = t.amount
   AND m.reversal_of_id IS NULL
   AND NOT EXISTS (SELECT 1 FROM estate_loan_transactions t2
                    WHERE t2.ledger_transaction_id = m.id);

-- Every loan gets an explicit opening 'borrow' row (initial drawdown) so schedules are movement based.
INSERT INTO estate_loan_transactions
    (id, loan_id, transaction_type, last_amount, new_amount, transaction_date, amount, interest_amount,
     ledger_transaction_id, created_at, updated_at)
SELECT gen_random_uuid()::text, l.loan_id, 'borrow', 0, l.principal_amount, l.issued_date, l.principal_amount, 0,
       (SELECT m.id FROM monetary_asset_transactions m
         WHERE m.reference_type = 'ESTATE_LOAN' AND m.reference_id = l.loan_id
           AND m.category = 'ESTATE_LOAN_RECEIPT' AND m.reversal_of_id IS NULL
         ORDER BY m.seq LIMIT 1),
       COALESCE(l.created_at, CURRENT_TIMESTAMP) - INTERVAL '1 second', CURRENT_TIMESTAMP
  FROM estate_loans l
 WHERE NOT EXISTS (SELECT 1 FROM estate_loan_transactions t
                    WHERE t.loan_id = l.loan_id AND t.transaction_type = 'borrow');

-- Movement order (seq): by creation time, the opening borrow before anything else on the same loan.
UPDATE estate_loan_transactions t SET seq = r.rn
  FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, (transaction_type <> 'borrow'), id) AS rn
          FROM estate_loan_transactions) r
 WHERE t.id = r.id;
SELECT setval(pg_get_serial_sequence('estate_loan_transactions', 'seq'),
              COALESCE((SELECT MAX(seq) FROM estate_loan_transactions), 1));

CREATE INDEX idx_elt_loan_date ON estate_loan_transactions (loan_id, transaction_date);
