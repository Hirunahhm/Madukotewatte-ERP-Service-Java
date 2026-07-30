CREATE TABLE fixed_assets (
    asset_id           VARCHAR(36)    PRIMARY KEY,
    category           VARCHAR(30)    NOT NULL,
    name               VARCHAR(255)   NOT NULL,
    acquisition_date   DATE           NOT NULL,
    acquisition_value  DECIMAL(15,2)  NOT NULL,
    current_value      DECIMAL(15,2)  NOT NULL,
    status             VARCHAR(20)    NOT NULL DEFAULT 'active',
    location           VARCHAR(255),
    notes              TEXT,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
