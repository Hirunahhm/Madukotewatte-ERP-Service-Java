-- ============================================================
-- V13__crop_production_records.sql
-- Adds harvest/production tracking for Banana, Coconut, and Manioc,
-- mirroring the latex_records/rubber_solid_records pattern: each
-- harvest ties to a Load (loadType already supports banana/coconut/
-- manioc — see LoadDialog) and an Employee, with a variety column
-- constrained per crop (plus a free-text note for the 'Other' escape
-- hatch, avoiding a migration every time an uncommon cultivar shows
-- up). Optionally auto-generates a payroll EmployeeTransaction the
-- same way rate_per_tree does for latex tapping (rate_per_* defaults
-- to 0, so nothing changes until an admin configures a rate).
-- ============================================================

ALTER TABLE employees ADD COLUMN rate_per_bunch DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE employees ADD COLUMN rate_per_nut DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE employees ADD COLUMN rate_per_kg_manioc DECIMAL(10, 2) NOT NULL DEFAULT 0;

ALTER TABLE employee_transactions DROP CONSTRAINT employee_transactions_type_check;
ALTER TABLE employee_transactions ADD CONSTRAINT employee_transactions_type_check
    CHECK (type IN ('Manual_Labor', 'Advance', 'Loan_Payment', 'Latex_Tap', 'Banana_Harvest', 'Coconut_Harvest', 'Manioc_Harvest'));

CREATE TABLE banana_records (
    record_id               VARCHAR(36)     PRIMARY KEY,
    load_id                 VARCHAR(36)     REFERENCES load_table(load_id),
    employee_id             VARCHAR(36)     REFERENCES employees(employee_id),
    transaction_record_id   VARCHAR(36)     REFERENCES employee_transactions(transaction_record_id),
    variety                 VARCHAR(30)     NOT NULL CHECK (variety IN ('Kolikuttu', 'Ambul', 'Seeni Kesel', 'Anamalu', 'Rathkesel', 'Suwandel', 'Other')),
    variety_note            VARCHAR(50),
    bunch_count             INT,
    mass_kg                 DECIMAL(10, 2),
    timestamp               TIMESTAMP,
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE coconut_records (
    record_id               VARCHAR(36)     PRIMARY KEY,
    load_id                 VARCHAR(36)     REFERENCES load_table(load_id),
    employee_id             VARCHAR(36)     REFERENCES employees(employee_id),
    transaction_record_id   VARCHAR(36)     REFERENCES employee_transactions(transaction_record_id),
    variety                 VARCHAR(30)     NOT NULL CHECK (variety IN ('Sri Lanka Tall', 'King Coconut (Thembili)', 'Green Dwarf', 'Yellow Dwarf', 'CRIC-65', 'San Ramon', 'Other')),
    variety_note            VARCHAR(50),
    nut_count               INT,
    mass_kg                 DECIMAL(10, 2),
    timestamp               TIMESTAMP,
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE manioc_records (
    record_id               VARCHAR(36)     PRIMARY KEY,
    load_id                 VARCHAR(36)     REFERENCES load_table(load_id),
    employee_id             VARCHAR(36)     REFERENCES employees(employee_id),
    transaction_record_id   VARCHAR(36)     REFERENCES employee_transactions(transaction_record_id),
    variety                 VARCHAR(30)     NOT NULL CHECK (variety IN ('Kirikawadi', 'MU-51', 'Suranimala', 'Ambakumbura', 'CARI-555', 'Other')),
    variety_note            VARCHAR(50),
    mass_kg                 DECIMAL(10, 2)  NOT NULL,
    timestamp               TIMESTAMP,
    created_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_banana_records_load ON banana_records(load_id);
CREATE INDEX idx_coconut_records_load ON coconut_records(load_id);
CREATE INDEX idx_manioc_records_load ON manioc_records(load_id);
