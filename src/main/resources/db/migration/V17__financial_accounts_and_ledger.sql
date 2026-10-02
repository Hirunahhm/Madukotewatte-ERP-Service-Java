-- V17: financial accounts + single account ledger (see ERP_E2E_PLAN.md section 2)
--   * financial_accounts replaces the hardcoded asset-type list and credit_card_limits
--   * monetary_asset_transactions becomes THE ledger for cash, bank and credit-card accounts
--   * credit-card rows of estate_loan_transactions move into the ledger (balance negative when owed)
--   * every free-text payment type column becomes an account_code FK

-- ── 1. financial_accounts ───────────────────────────────────────────────────
CREATE TABLE financial_accounts (
    code            VARCHAR(40)   PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    type            VARCHAR(12)   NOT NULL,
    institution     VARCHAR(100),
    account_number  VARCHAR(50),
    credit_limit    NUMERIC(15,2),
    opening_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
    opening_date    DATE          NOT NULL DEFAULT CURRENT_DATE,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT financial_accounts_type_check CHECK (type IN ('CASH', 'BANK', 'CREDIT_CARD')),
    CONSTRAINT financial_accounts_limit_check CHECK (
        (type = 'CREDIT_CARD' AND credit_limit IS NOT NULL AND credit_limit >= 0)
        OR (type <> 'CREDIT_CARD' AND credit_limit IS NULL))
);

INSERT INTO financial_accounts (code, name, type, institution) VALUES
    ('Cash',         'Cash in hand',        'CASH', NULL),
    ('Bank-BOC',     'Bank of Ceylon',      'BANK', 'Bank of Ceylon'),
    ('Bank-Peoples', 'People''s Bank',      'BANK', 'People''s Bank'),
    ('Bank-Seylan',  'Seylan Bank',         'BANK', 'Seylan Bank');

INSERT INTO financial_accounts (code, name, type, institution, credit_limit) VALUES
    ('CC-Peoples', 'People''s Bank credit card', 'CREDIT_CARD', 'People''s Bank',
        COALESCE((SELECT credit_limit FROM credit_card_limits WHERE loan_type = 'credit-card - Peoples'), 0)),
    ('CC-Sampath', 'Sampath Bank credit card',   'CREDIT_CARD', 'Sampath Bank',
        COALESCE((SELECT credit_limit FROM credit_card_limits WHERE loan_type = 'credit-card - Sampath'), 0));

DROP TABLE credit_card_limits;

-- Maps the old free-text payment types onto account codes (NULL when unknown / not paid).
CREATE FUNCTION pg_temp.map_account(v TEXT) RETURNS TEXT AS $fn$
    SELECT CASE
        WHEN v IS NULL OR btrim(v) = '' THEN NULL
        WHEN lower(v) = 'cash' THEN 'Cash'
        WHEN v IN ('Bank Transfer-BOC', 'bank_transfer', 'Bank-BOC') THEN 'Bank-BOC'
        WHEN v IN ('Bank Transfer-Seylan', 'Bank-Seylan') THEN 'Bank-Seylan'
        WHEN v IN ('Bank Transfer-Peoples', 'Bank-Peoples') THEN 'Bank-Peoples'
        WHEN v IN ('Credit Card-Peoples', 'credit-card - Peoples') THEN 'CC-Peoples'
        WHEN v IN ('Credit Card-Sampath', 'credit-card - Sampath') THEN 'CC-Sampath'
        ELSE NULL
    END
$fn$ LANGUAGE sql IMMUTABLE;

-- ── 2. Reshape the ledger ───────────────────────────────────────────────────
ALTER TABLE monetary_asset_transactions DROP CONSTRAINT monetary_asset_transactions_asset_type_check;
ALTER TABLE monetary_asset_transactions RENAME COLUMN asset_type TO account_code;
ALTER TABLE monetary_asset_transactions ALTER COLUMN account_code TYPE VARCHAR(40);
ALTER TABLE monetary_asset_transactions
    ADD CONSTRAINT monetary_asset_transactions_account_fk
    FOREIGN KEY (account_code) REFERENCES financial_accounts(code);

ALTER TABLE monetary_asset_transactions
    ADD COLUMN amount           NUMERIC(15,2),
    ADD COLUMN transaction_date DATE,
    ADD COLUMN category         VARCHAR(30),
    ADD COLUMN reference_type   VARCHAR(40),
    ADD COLUMN reference_id     VARCHAR(36),
    ADD COLUMN description      TEXT,
    ADD COLUMN reversal_of_id   VARCHAR(36) REFERENCES monetary_asset_transactions(id),
    ADD COLUMN seq              BIGINT;

UPDATE monetary_asset_transactions
SET amount = ABS(COALESCE(new_amount, 0) - COALESCE(last_amount, 0)),
    transaction_date = created_at::date;

-- deterministic insertion order for running balances (created_at can tie within one transaction)
CREATE SEQUENCE monetary_asset_transactions_seq;
UPDATE monetary_asset_transactions m SET seq = r.rn
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn FROM monetary_asset_transactions) r
WHERE r.id = m.id;
SELECT setval('monetary_asset_transactions_seq', COALESCE((SELECT MAX(seq) FROM monetary_asset_transactions), 0) + 1, false);
ALTER TABLE monetary_asset_transactions ALTER COLUMN seq SET DEFAULT nextval('monetary_asset_transactions_seq');
ALTER TABLE monetary_asset_transactions ALTER COLUMN seq SET NOT NULL;
ALTER SEQUENCE monetary_asset_transactions_seq OWNED BY monetary_asset_transactions.seq;

-- Categorise existing rows ---------------------------------------------------
UPDATE monetary_asset_transactions m
SET category = 'SALE_RECEIPT', reference_type = 'SALE_LATEX', reference_id = s.sale_id
FROM sales_latex s
WHERE s.transaction_id = m.id AND m.category IS NULL;

UPDATE monetary_asset_transactions m
SET category = CASE WHEN e.type = 'Labor' THEN 'LABOUR_PAYMENT' ELSE 'EXPENSE_PAYMENT' END,
    reference_type = 'EXPENSE', reference_id = e.expense_id
FROM expenses e
WHERE e.transaction_id = m.id AND m.category IS NULL;

-- salary payments: the old code posted the ledger row and the employee transaction in one request
UPDATE monetary_asset_transactions m
SET category = 'SALARY_PAYMENT', reference_type = 'EMPLOYEE_TRANSACTION', reference_id = x.tid
FROM (
    SELECT DISTINCT ON (m2.id) m2.id AS mid, t.transaction_record_id AS tid
    FROM monetary_asset_transactions m2
    JOIN employee_transactions t
      ON t.type = 'Salary_Payment'
     AND m2.category IS NULL
     AND m2.transaction_type = 'money out'
     AND m2.amount = t.amount
     AND ABS(EXTRACT(EPOCH FROM (m2.created_at - t.created_at))) < 30
    ORDER BY m2.id, ABS(EXTRACT(EPOCH FROM (m2.created_at - t.created_at)))
) x
WHERE m.id = x.mid AND m.category IS NULL;

-- named estate-loan repayments, same technique
UPDATE monetary_asset_transactions m
SET category = 'ESTATE_LOAN_REPAYMENT', reference_type = 'ESTATE_LOAN', reference_id = x.lid
FROM (
    SELECT DISTINCT ON (m2.id) m2.id AS mid, t.loan_id AS lid
    FROM monetary_asset_transactions m2
    JOIN estate_loan_transactions t
      ON t.loan_id IS NOT NULL
     AND t.transaction_type = 'repay'
     AND m2.category IS NULL
     AND m2.transaction_type = 'money out'
     AND m2.amount = ABS(COALESCE(t.new_amount, 0) - COALESCE(t.last_amount, 0))
     AND ABS(EXTRACT(EPOCH FROM (m2.created_at - t.created_at))) < 30
    ORDER BY m2.id, ABS(EXTRACT(EPOCH FROM (m2.created_at - t.created_at)))
) x
WHERE m.id = x.mid AND m.category IS NULL;

UPDATE monetary_asset_transactions
SET category = 'OPENING_BALANCE', reference_type = 'ACCOUNT', reference_id = account_code,
    description = 'Migrated opening balance'
WHERE category IS NULL AND transaction_type = 'money in';

UPDATE monetary_asset_transactions
SET category = 'ADJUSTMENT', description = 'Migrated outflow (source unknown)'
WHERE category IS NULL AND transaction_type = 'money out';

-- ── 3. Credit-card rows of estate_loan_transactions -> ledger ──────────────
-- old balance is "amount owed" (positive); ledger balance is negative when owed.
INSERT INTO monetary_asset_transactions
    (id, transaction_type, account_code, amount, last_amount, new_amount, transaction_date,
     category, reference_type, reference_id, description, created_at, updated_at)
SELECT c.id,
       CASE WHEN c.transaction_type = 'borrow' THEN 'money out' ELSE 'money in' END,
       pg_temp.map_account(c.loan_type),
       ABS(COALESCE(c.new_amount, 0) - COALESCE(c.last_amount, 0)),
       -COALESCE(c.last_amount, 0),
       -COALESCE(c.new_amount, 0),
       c.created_at::date,
       CASE
           WHEN c.expense_id IS NOT NULL AND c.expense_type = 'Labor' THEN 'LABOUR_PAYMENT'
           WHEN c.expense_id IS NOT NULL THEN 'EXPENSE_PAYMENT'
           WHEN c.transaction_type = 'borrow' AND COALESCE(c.last_amount, 0) = 0 THEN 'OPENING_BALANCE'
           ELSE 'ADJUSTMENT'
       END,
       CASE WHEN c.expense_id IS NOT NULL THEN 'EXPENSE'
            WHEN c.transaction_type = 'borrow' AND COALESCE(c.last_amount, 0) = 0 THEN 'ACCOUNT' END,
       CASE WHEN c.expense_id IS NOT NULL THEN c.expense_id
            WHEN c.transaction_type = 'borrow' AND COALESCE(c.last_amount, 0) = 0 THEN pg_temp.map_account(c.loan_type) END,
       CASE WHEN c.expense_id IS NULL AND c.transaction_type = 'repay'
            THEN 'Migrated card payment (no bank leg recorded)' END,
       c.created_at, c.updated_at
FROM (
    SELECT DISTINCT ON (t.id) t.*, e.expense_id, e.type AS expense_type
    FROM estate_loan_transactions t
    LEFT JOIN expenses e ON e.estate_loan_transaction_id = t.id
    WHERE t.loan_type IS NOT NULL
    ORDER BY t.id
) c
ORDER BY c.created_at, c.id;

-- expenses paid by card now point at the ledger row (it kept the same id)
UPDATE expenses SET transaction_id = estate_loan_transaction_id
WHERE estate_loan_transaction_id IS NOT NULL AND transaction_id IS NULL;
ALTER TABLE expenses DROP CONSTRAINT expenses_estate_loan_transaction_id_fkey;
ALTER TABLE expenses DROP COLUMN estate_loan_transaction_id;

DELETE FROM estate_loan_transactions WHERE loan_type IS NOT NULL;
ALTER TABLE estate_loan_transactions DROP CONSTRAINT estate_loan_transactions_type_or_loan_check;
ALTER TABLE estate_loan_transactions DROP CONSTRAINT estate_loan_transactions_loan_type_check;
ALTER TABLE estate_loan_transactions DROP COLUMN loan_type;
ALTER TABLE estate_loan_transactions ALTER COLUMN loan_id SET NOT NULL;

-- ── 4. Ledger constraints ──────────────────────────────────────────────────
ALTER TABLE monetary_asset_transactions ALTER COLUMN amount SET NOT NULL;
ALTER TABLE monetary_asset_transactions ALTER COLUMN transaction_date SET NOT NULL;
ALTER TABLE monetary_asset_transactions ALTER COLUMN category SET NOT NULL;
ALTER TABLE monetary_asset_transactions
    ADD CONSTRAINT monetary_asset_transactions_amount_check CHECK (amount > 0);
ALTER TABLE monetary_asset_transactions
    ADD CONSTRAINT monetary_asset_transactions_category_check CHECK (category IN (
        'OPENING_BALANCE', 'OWNER_CAPITAL', 'OWNER_DRAWING', 'TRANSFER', 'SALE_RECEIPT',
        'OTHER_INCOME', 'INTEREST_INCOME', 'EXPENSE_PAYMENT', 'CREDIT_CARD_PAYMENT',
        'SALARY_PAYMENT', 'LABOUR_PAYMENT', 'EMPLOYEE_ADVANCE', 'EMPLOYEE_LOAN_ISSUE',
        'EMPLOYEE_LOAN_REPAYMENT', 'ESTATE_LOAN_RECEIPT', 'ESTATE_LOAN_REPAYMENT',
        'ASSET_PURCHASE', 'ASSET_DISPOSAL', 'BANK_CHARGES', 'ADJUSTMENT'));
CREATE UNIQUE INDEX uq_monetary_asset_transactions_seq ON monetary_asset_transactions (seq);
CREATE INDEX idx_mat_account_seq ON monetary_asset_transactions (account_code, seq DESC);
CREATE INDEX idx_mat_account_date ON monetary_asset_transactions (account_code, transaction_date);
CREATE INDEX idx_mat_category ON monetary_asset_transactions (category);
CREATE INDEX idx_mat_reference ON monetary_asset_transactions (reference_type, reference_id);
CREATE UNIQUE INDEX uq_mat_reversal_of ON monetary_asset_transactions (reversal_of_id) WHERE reversal_of_id IS NOT NULL;

-- opening balance of every account = its OPENING_BALANCE rows (signed)
UPDATE financial_accounts a
SET opening_balance = s.bal, opening_date = s.d
FROM (
    SELECT account_code,
           SUM(CASE WHEN transaction_type = 'money in' THEN amount ELSE -amount END) AS bal,
           MIN(transaction_date) AS d
    FROM monetary_asset_transactions
    WHERE category = 'OPENING_BALANCE'
    GROUP BY account_code
) s
WHERE s.account_code = a.code;

UPDATE financial_accounts a
SET opening_date = LEAST(CURRENT_DATE, m.d)
FROM (SELECT account_code, MIN(transaction_date) AS d FROM monetary_asset_transactions GROUP BY account_code) m
WHERE m.account_code = a.code
  AND NOT EXISTS (SELECT 1 FROM monetary_asset_transactions o
                  WHERE o.account_code = a.code AND o.category = 'OPENING_BALANCE');

-- ── 5. Payment-type text columns -> account_code FKs ────────────────────────
-- expenses
ALTER TABLE expenses DROP CONSTRAINT expenses_payment_type_check;
ALTER TABLE expenses RENAME COLUMN payment_type TO account_code;
ALTER TABLE expenses ALTER COLUMN account_code TYPE VARCHAR(40);
UPDATE expenses SET account_code = pg_temp.map_account(account_code);
UPDATE expenses e SET account_code = m.account_code
FROM monetary_asset_transactions m WHERE e.transaction_id = m.id;
ALTER TABLE expenses ADD CONSTRAINT expenses_account_fk FOREIGN KEY (account_code) REFERENCES financial_accounts(code);
ALTER TABLE expenses ADD COLUMN reference_type VARCHAR(40), ADD COLUMN reference_id VARCHAR(36);
-- system-created labour expenses: trace back to the labour record
UPDATE expenses e
SET reference_type = 'LABOUR',
    reference_id = (SELECT l.labour_id FROM labour l
                    WHERE l.is_paid AND l.amount = e.amount AND l.timestamp = e.timestamp LIMIT 1)
WHERE e.type = 'Labor';
UPDATE expenses SET type = 'Labour' WHERE type = 'Labor';
CREATE INDEX idx_expenses_reference ON expenses (reference_type, reference_id);

-- labour
ALTER TABLE labour DROP CONSTRAINT labour_payment_type_check;
ALTER TABLE labour RENAME COLUMN payment_type TO account_code;
ALTER TABLE labour ALTER COLUMN account_code TYPE VARCHAR(40);
UPDATE labour SET account_code = pg_temp.map_account(account_code);
ALTER TABLE labour ADD CONSTRAINT labour_account_fk FOREIGN KEY (account_code) REFERENCES financial_accounts(code);

-- employee_transactions
ALTER TABLE employee_transactions DROP CONSTRAINT employee_transactions_payment_type_check;
ALTER TABLE employee_transactions RENAME COLUMN payment_type TO account_code;
ALTER TABLE employee_transactions ALTER COLUMN account_code TYPE VARCHAR(40);
UPDATE employee_transactions SET account_code = pg_temp.map_account(account_code);
UPDATE employee_transactions t SET account_code = m.account_code
FROM monetary_asset_transactions m WHERE m.reference_type = 'EMPLOYEE_TRANSACTION' AND m.reference_id = t.transaction_record_id;
ALTER TABLE employee_transactions ADD CONSTRAINT employee_transactions_account_fk
    FOREIGN KEY (account_code) REFERENCES financial_accounts(code);

-- ── 6. Sales tables: account_code, payment_date, ledger link ─────────────────
-- sales_latex had no payment_type column at all
ALTER TABLE sales_latex
    ADD COLUMN account_code VARCHAR(40) REFERENCES financial_accounts(code),
    ADD COLUMN payment_date DATE;
UPDATE sales_latex s
SET account_code = m.account_code, payment_date = m.transaction_date
FROM monetary_asset_transactions m WHERE s.transaction_id = m.id;
UPDATE sales_latex SET payment_date = updated_at::date
WHERE is_payment_received AND payment_date IS NULL;

ALTER TABLE sales_rubber_solid RENAME COLUMN payment_type TO account_code;
ALTER TABLE sales_manioc       RENAME COLUMN payment_type TO account_code;
ALTER TABLE sales_coconut      RENAME COLUMN payment_type TO account_code;
ALTER TABLE sales_banana       RENAME COLUMN payment_type TO account_code;

ALTER TABLE sales_rubber_solid ALTER COLUMN account_code TYPE VARCHAR(40);
ALTER TABLE sales_manioc       ALTER COLUMN account_code TYPE VARCHAR(40);
ALTER TABLE sales_coconut      ALTER COLUMN account_code TYPE VARCHAR(40);
ALTER TABLE sales_banana       ALTER COLUMN account_code TYPE VARCHAR(40);

UPDATE sales_rubber_solid SET account_code = pg_temp.map_account(account_code);
UPDATE sales_manioc       SET account_code = pg_temp.map_account(account_code);
UPDATE sales_coconut      SET account_code = pg_temp.map_account(account_code);
UPDATE sales_banana       SET account_code = pg_temp.map_account(account_code);

ALTER TABLE sales_rubber_solid
    ADD CONSTRAINT sales_rubber_solid_account_fk FOREIGN KEY (account_code) REFERENCES financial_accounts(code),
    ADD COLUMN transaction_id VARCHAR(36) REFERENCES monetary_asset_transactions(id),
    ADD COLUMN payment_date DATE;
ALTER TABLE sales_manioc
    ADD CONSTRAINT sales_manioc_account_fk FOREIGN KEY (account_code) REFERENCES financial_accounts(code),
    ADD COLUMN transaction_id VARCHAR(36) REFERENCES monetary_asset_transactions(id),
    ADD COLUMN payment_date DATE;
ALTER TABLE sales_coconut
    ADD CONSTRAINT sales_coconut_account_fk FOREIGN KEY (account_code) REFERENCES financial_accounts(code),
    ADD COLUMN transaction_id VARCHAR(36) REFERENCES monetary_asset_transactions(id),
    ADD COLUMN payment_date DATE;
ALTER TABLE sales_banana
    ADD CONSTRAINT sales_banana_account_fk FOREIGN KEY (account_code) REFERENCES financial_accounts(code),
    ADD COLUMN transaction_id VARCHAR(36) REFERENCES monetary_asset_transactions(id),
    ADD COLUMN payment_date DATE;

UPDATE sales_rubber_solid SET payment_date = updated_at::date WHERE is_paid;
UPDATE sales_manioc       SET payment_date = updated_at::date WHERE is_paid;
UPDATE sales_coconut      SET payment_date = updated_at::date WHERE is_paid;
UPDATE sales_banana       SET payment_date = updated_at::date WHERE is_paid;
