-- ============================================================
-- V1__init_schema.sql
-- Madukotawatte Estate ERP - Initial DB Schema
-- ============================================================

-- 1. Financial Foundation (Assets & External Liabilities) ----------

CREATE TABLE monetary_asset_transactions (
    id              VARCHAR(36)        PRIMARY KEY,
    transaction_type VARCHAR(10)    NOT NULL CHECK (transaction_type IN ('money in', 'money out')),
    asset_type      VARCHAR(20)     NOT NULL CHECK (asset_type IN ('Cash', 'Bank-BOC', 'Bank-Peoples', 'Bank-Seylan')),
    last_amount     DECIMAL(15, 2),
    new_amount      DECIMAL(15, 2),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE estate_loan_transactions (
    id              VARCHAR(36)        PRIMARY KEY,
    loan_type       VARCHAR(30)     NOT NULL CHECK (loan_type IN ('credit-card - Peoples', 'credit-card - Sampath', 'Loan-mom', 'Loan-other')),
    last_amount     DECIMAL(15, 2),
    new_amount      DECIMAL(15, 2),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 2. Workforce Management -------------------------------------------

CREATE TABLE employees (
    employee_id     VARCHAR(36)        PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    joined_date     DATE,
    salary          DECIMAL(10, 2),
    position        VARCHAR(100),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    user_id         VARCHAR(36)        PRIMARY KEY,
    username        VARCHAR(50)     UNIQUE NOT NULL,
    email           VARCHAR(100)    UNIQUE NOT NULL,
    pw_hash         VARCHAR(255)    NOT NULL,
    role            VARCHAR(50),
    employee_id     VARCHAR(36)        REFERENCES employees(employee_id),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_loans (
    loan_id             VARCHAR(36)        PRIMARY KEY,
    employee_id         VARCHAR(36)        REFERENCES employees(employee_id),
    principal_amount    DECIMAL(15, 2),
    interest            DECIMAL(5, 2),
    installment         DECIMAL(10, 2),
    current_balance     DECIMAL(15, 2),
    is_active           BOOLEAN         DEFAULT TRUE,
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 3. Field Operations & Inventory ----------------------------------

CREATE TABLE load_table (
    load_id         VARCHAR(36)        PRIMARY KEY,
    load_type       VARCHAR(50),
    start_date      TIMESTAMP,
    end_date        TIMESTAMP,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attendance (
    attendance_id   VARCHAR(36)        PRIMARY KEY,
    employee_id     VARCHAR(36)        REFERENCES employees(employee_id),
    timestamp       TIMESTAMP,
    no_of_trees     INT,
    no_work         VARCHAR(10)     DEFAULT 'none' CHECK (no_work IN ('none', 'rain', 'ill', 'no_loads', 'holiday')),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE metrolac_readings (
    metrolac_id     VARCHAR(36)        PRIMARY KEY,
    load_id         VARCHAR(36)        REFERENCES load_table(load_id),
    temperature     DECIMAL(5, 2),
    timestamp       TIMESTAMP,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE latex_records (
    record_id       VARCHAR(36)        PRIMARY KEY,
    load_id         VARCHAR(36)        REFERENCES load_table(load_id),
    employee_id     VARCHAR(36)        REFERENCES employees(employee_id),
    timestamp       TIMESTAMP,
    latex_amount    DECIMAL(10, 2),
    ammonia_amount  DECIMAL(10, 2),
    metrolac_id     VARCHAR(36)        REFERENCES metrolac_readings(metrolac_id),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ammonia_records (
    record_id       VARCHAR(36)        PRIMARY KEY,
    type            VARCHAR(10)     CHECK (type IN ('Refill', 'Out')),
    litres          DECIMAL(10, 2),
    timestamp       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rubber_solid_records (
    record_id       VARCHAR(36)        PRIMARY KEY,
    load_id         VARCHAR(36)        REFERENCES load_table(load_id),
    mass_kg         DECIMAL(10, 2),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 4. Financial Transactions ----------------------------------------

CREATE TABLE sales_latex (
    sale_id                 VARCHAR(36)        PRIMARY KEY,
    load_id                 VARCHAR(36)        REFERENCES load_table(load_id),
    mass                    DECIMAL(10, 2),
    litres                  DECIMAL(10, 2),
    metrolac_reading        DECIMAL(10, 2),
    unit_price              DECIMAL(10, 2),
    total_amount            DECIMAL(15, 2),
    is_payment_received     BOOLEAN         DEFAULT FALSE,
    transaction_id          VARCHAR(36)        REFERENCES monetary_asset_transactions(id),
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expenses (
    expense_id                  VARCHAR(36)        PRIMARY KEY,
    type                        VARCHAR(100),
    payment_type                VARCHAR(40)     NOT NULL CHECK (payment_type IN ('Credit Card-Peoples', 'Cash', 'Bank Transfer-BOC', 'Bank Transfer-Seylan', 'Bank Transfer-Peoples')),
    amount                      DECIMAL(15, 2),
    transaction_id              VARCHAR(36)        REFERENCES monetary_asset_transactions(id),
    estate_loan_transaction_id  VARCHAR(36)        REFERENCES estate_loan_transactions(id),
    timestamp                   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_transactions (
    transaction_record_id   VARCHAR(36)        PRIMARY KEY,
    employee_id             VARCHAR(36)        REFERENCES employees(employee_id),
    type                    VARCHAR(20)     CHECK (type IN ('Manual_Labor', 'Advance', 'Loan_Payment', 'Latex_Tap')),
    amount                  DECIMAL(15, 2),
    timestamp               TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 5. Indexes for performance ---------------------------------------
CREATE INDEX idx_attendance_employee ON attendance(employee_id);
CREATE INDEX idx_attendance_timestamp ON attendance(timestamp);
CREATE INDEX idx_latex_records_load ON latex_records(load_id);
CREATE INDEX idx_latex_records_employee ON latex_records(employee_id);
CREATE INDEX idx_employee_transactions_employee ON employee_transactions(employee_id);
CREATE INDEX idx_expenses_timestamp ON expenses(timestamp);
CREATE INDEX idx_sales_latex_load ON sales_latex(load_id);
