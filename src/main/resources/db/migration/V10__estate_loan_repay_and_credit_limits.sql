ALTER TABLE estate_loan_transactions ADD COLUMN transaction_type VARCHAR(10) NOT NULL DEFAULT 'borrow';

CREATE TABLE credit_card_limits (
    loan_type    VARCHAR(30)   PRIMARY KEY,
    credit_limit DECIMAL(15,2) NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO credit_card_limits (loan_type, credit_limit) VALUES
    ('credit-card - Peoples', 500000.00),
    ('credit-card - Sampath', 500000.00);
