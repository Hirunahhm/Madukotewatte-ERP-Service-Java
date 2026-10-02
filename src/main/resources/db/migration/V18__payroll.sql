-- V18: payroll & workforce — pay types, advances, employee loans with ledger links, payslips.

-- 1. Employee pay type -------------------------------------------------------
ALTER TABLE employees ADD COLUMN pay_type VARCHAR(20) NOT NULL DEFAULT 'FIXED_MONTHLY';
ALTER TABLE employees ADD COLUMN hourly_rate DECIMAL(10, 2);
UPDATE employees SET pay_type = 'PER_TREE' WHERE position ILIKE '%tapper%';
ALTER TABLE employees ADD CONSTRAINT employees_pay_type_check
    CHECK (pay_type IN ('FIXED_MONTHLY', 'PER_TREE', 'HOURLY'));

-- 2. Payslips ----------------------------------------------------------------
CREATE TABLE payslips (
    payslip_id            VARCHAR(36)    PRIMARY KEY,
    employee_id           VARCHAR(36)    NOT NULL REFERENCES employees(employee_id),
    period_start          DATE           NOT NULL,
    period_end            DATE           NOT NULL,
    fixed_salary          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tap_earnings          DECIMAL(15, 2) NOT NULL DEFAULT 0,
    hourly_earnings       DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_earnings        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_earnings_note   VARCHAR(255),
    gross_pay             DECIMAL(15, 2) NOT NULL,
    advance_deduction     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    loan_deduction        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_deduction       DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_deduction_note  VARCHAR(255),
    net_pay               DECIMAL(15, 2) NOT NULL,
    account_code          VARCHAR(40)    NOT NULL REFERENCES financial_accounts(code),
    payment_date          DATE           NOT NULL,
    transaction_id        VARCHAR(36)    REFERENCES monetary_asset_transactions(id),
    loan_id               VARCHAR(36)    REFERENCES employee_loans(loan_id),
    notes                 TEXT,
    status                VARCHAR(10)    NOT NULL DEFAULT 'PAID' CHECK (status IN ('PAID', 'VOID')),
    void_reason           VARCHAR(255),
    voided_at             TIMESTAMP,
    created_at            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payslips_period_check CHECK (period_end >= period_start)
);
CREATE INDEX idx_payslips_employee ON payslips(employee_id, period_start, period_end);
CREATE INDEX idx_payslips_payment_date ON payslips(payment_date);

-- 3. Employee transactions: ledger link, note, payslip link, Advance_Recovery type
ALTER TABLE employee_transactions ADD COLUMN transaction_id VARCHAR(36) REFERENCES monetary_asset_transactions(id);
ALTER TABLE employee_transactions ADD COLUMN note VARCHAR(255);
ALTER TABLE employee_transactions ADD COLUMN payslip_id VARCHAR(36) REFERENCES payslips(payslip_id);
ALTER TABLE employee_transactions DROP CONSTRAINT employee_transactions_type_check;
ALTER TABLE employee_transactions ADD CONSTRAINT employee_transactions_type_check
    CHECK (type IN ('Manual_Labor', 'Advance', 'Loan_Payment', 'Latex_Tap', 'Salary_Payment', 'Advance_Recovery'));
CREATE INDEX idx_employee_transactions_payslip ON employee_transactions(payslip_id);

-- legacy salary payments / advances that already moved the ledger keep their link
UPDATE employee_transactions t SET transaction_id = m.id
FROM monetary_asset_transactions m
WHERE m.reference_type = 'EMPLOYEE_TRANSACTION' AND m.reference_id = t.transaction_record_id
  AND m.reversal_of_id IS NULL AND t.transaction_id IS NULL;

-- 4. Labour paid via payslip
ALTER TABLE labour ADD COLUMN payslip_id VARCHAR(36) REFERENCES payslips(payslip_id);
CREATE INDEX idx_labour_payslip ON labour(payslip_id);

-- 5. Employee loans: issue date + ledger link; repayments table
ALTER TABLE employee_loans ADD COLUMN issued_date DATE;
ALTER TABLE employee_loans ADD COLUMN transaction_id VARCHAR(36) REFERENCES monetary_asset_transactions(id);
UPDATE employee_loans SET issued_date = created_at::date WHERE issued_date IS NULL;
ALTER TABLE employee_loans ALTER COLUMN issued_date SET NOT NULL;

CREATE TABLE employee_loan_repayments (
    repayment_id    VARCHAR(36)    PRIMARY KEY,
    loan_id         VARCHAR(36)    NOT NULL REFERENCES employee_loans(loan_id),
    amount          DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    repayment_date  DATE           NOT NULL,
    source          VARCHAR(10)    NOT NULL CHECK (source IN ('PAYSLIP', 'CASH')),
    payslip_id      VARCHAR(36)    REFERENCES payslips(payslip_id),
    transaction_id  VARCHAR(36)    REFERENCES monetary_asset_transactions(id),
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_loan_repayments_loan ON employee_loan_repayments(loan_id);
