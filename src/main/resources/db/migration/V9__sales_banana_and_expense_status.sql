-- Sales Banana (new) — mirrors sales_manioc / sales_coconut exactly
CREATE TABLE sales_banana (
    sale_id       VARCHAR(36)   PRIMARY KEY,
    load_id       VARCHAR(36)   NOT NULL REFERENCES load_table(load_id),
    type          VARCHAR(50)   NOT NULL,
    sale_date     DATE          NOT NULL,
    mass          DECIMAL(10,2) NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    is_paid       BOOLEAN       NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)   NOT NULL DEFAULT 'pending',
    payment_type  VARCHAR(50),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sales_banana_load ON sales_banana(load_id);

-- Expenses: support a pending/paid workflow (previously every expense required
-- payment_type + a ledger transaction at creation, i.e. was always "already paid")
ALTER TABLE expenses ALTER COLUMN payment_type DROP NOT NULL;
ALTER TABLE expenses ADD COLUMN is_paid BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE expenses ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'paid';
