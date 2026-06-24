-- 1. load_table: add status
ALTER TABLE load_table ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending';

-- 2. ammonia_records: swap litres → previous_amount + new_amount
ALTER TABLE ammonia_records DROP COLUMN litres;
ALTER TABLE ammonia_records ADD COLUMN previous_amount DECIMAL(10,2);
ALTER TABLE ammonia_records ADD COLUMN new_amount DECIMAL(10,2);

-- 3. calendar table (new)
CREATE TABLE calendar (
    calendar_id   VARCHAR(36)  PRIMARY KEY,
    calendar_date DATE         NOT NULL UNIQUE,
    is_holiday    BOOLEAN      NOT NULL DEFAULT FALSE,
    rained        BOOLEAN      NOT NULL DEFAULT FALSE,
    description   VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. attendance: add calendar FK (nullable)
ALTER TABLE attendance ADD COLUMN calendar_id VARCHAR(36);
ALTER TABLE attendance ADD CONSTRAINT fk_attendance_calendar
    FOREIGN KEY (calendar_id) REFERENCES calendar(calendar_id);

-- 5. employees: add is_active
ALTER TABLE employees ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 6. labour table (new)
CREATE TABLE labour (
    labour_id              VARCHAR(36)    PRIMARY KEY,
    employee_id            VARCHAR(36)    NOT NULL REFERENCES employees(employee_id),
    transaction_record_id  VARCHAR(36)    REFERENCES employee_transactions(transaction_record_id),
    is_paid                BOOLEAN        NOT NULL DEFAULT FALSE,
    worked_hours           DECIMAL(6,2)   NOT NULL,
    hourly_rate            DECIMAL(10,2)  NOT NULL,
    amount                 DECIMAL(15,2)  NOT NULL,
    work_type              VARCHAR(50)    NOT NULL,
    description            TEXT,
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_labour_employee ON labour(employee_id);

-- 7. employee_transactions: add payment_type
ALTER TABLE employee_transactions ADD COLUMN payment_type VARCHAR(20)
    CHECK (payment_type IN ('bank_transfer', 'cash'));

-- 8. sales_rubber_solid table (new)
CREATE TABLE sales_rubber_solid (
    sale_id       VARCHAR(36)    PRIMARY KEY,
    load_id       VARCHAR(36)    NOT NULL REFERENCES load_table(load_id),
    sale_date     DATE           NOT NULL,
    mass          DECIMAL(10,2)  NOT NULL,
    unit_price    DECIMAL(10,2)  NOT NULL,
    is_paid       BOOLEAN        NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)    NOT NULL DEFAULT 'pending',
    payment_type  VARCHAR(50),
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sales_rubber_solid_load ON sales_rubber_solid(load_id);

-- 9. vehicles table (new)
CREATE TABLE vehicles (
    vehicle_id       VARCHAR(36)   PRIMARY KEY,
    registration_no  VARCHAR(50)   NOT NULL UNIQUE,
    make             VARCHAR(100),
    model            VARCHAR(100),
    year             INT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'active',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. vehicle_maintenance_records table (new)
CREATE TABLE vehicle_maintenance_records (
    maintenance_id    VARCHAR(36)   PRIMARY KEY,
    vehicle_id        VARCHAR(36)   NOT NULL REFERENCES vehicles(vehicle_id),
    expense_id        VARCHAR(36)   REFERENCES expenses(expense_id),
    maintenance_date  DATE          NOT NULL,
    description       TEXT,
    cost              DECIMAL(15,2),
    service_provider  VARCHAR(255),
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicle_maintenance_vehicle ON vehicle_maintenance_records(vehicle_id);

-- 11. sales_manioc table (new)
CREATE TABLE sales_manioc (
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
CREATE INDEX idx_sales_manioc_load ON sales_manioc(load_id);

-- 12. sales_coconut table (new)
CREATE TABLE sales_coconut (
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
CREATE INDEX idx_sales_coconut_load ON sales_coconut(load_id);
