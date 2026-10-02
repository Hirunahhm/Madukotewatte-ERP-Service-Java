-- Wave 2b: general product sales, buyer/due dates, expense payee/description/due date + standard categories,
-- cocoa & other crop harvests.

-- General product sales (cocoa, pepper, cinnamon, ...)
CREATE TABLE sales_general (
    sale_id        VARCHAR(36)    PRIMARY KEY,
    product        VARCHAR(30)    NOT NULL,
    description    VARCHAR(255),
    quantity       NUMERIC(12, 3) NOT NULL,
    unit           VARCHAR(20)    NOT NULL,
    unit_price     NUMERIC(12, 2) NOT NULL,
    total_amount   NUMERIC(15, 2) NOT NULL,
    buyer_name     VARCHAR(150),
    sale_date      DATE           NOT NULL,
    due_date       DATE,
    is_paid        BOOLEAN        NOT NULL DEFAULT FALSE,
    status         VARCHAR(20)    NOT NULL DEFAULT 'pending',
    account_code   VARCHAR(40)    REFERENCES financial_accounts(code),
    payment_date   DATE,
    transaction_id VARCHAR(36)    REFERENCES monetary_asset_transactions(id),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_general_product_check CHECK (product IN
        ('Cocoa','Pepper','Cinnamon','Coffee','Arecanut','Cloves','Timber','Firewood','Other')),
    CONSTRAINT sales_general_unit_check CHECK (unit IN
        ('kg','g','nuts','bunches','litres','units','cubic_ft','other')),
    CONSTRAINT sales_general_amounts_check CHECK (quantity > 0 AND unit_price >= 0 AND total_amount >= 0)
);
CREATE INDEX idx_sales_general_date ON sales_general (sale_date);
CREATE INDEX idx_sales_general_unpaid ON sales_general (is_paid);

-- Buyer + due date on every sales table
ALTER TABLE sales_latex        ADD COLUMN buyer_name VARCHAR(150), ADD COLUMN due_date DATE;
ALTER TABLE sales_rubber_solid ADD COLUMN buyer_name VARCHAR(150), ADD COLUMN due_date DATE;
ALTER TABLE sales_manioc       ADD COLUMN buyer_name VARCHAR(150), ADD COLUMN due_date DATE;
ALTER TABLE sales_coconut      ADD COLUMN buyer_name VARCHAR(150), ADD COLUMN due_date DATE;
ALTER TABLE sales_banana       ADD COLUMN buyer_name VARCHAR(150), ADD COLUMN due_date DATE;

-- Expenses: payee, description, due date
ALTER TABLE expenses
    ADD COLUMN payee       VARCHAR(150),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN due_date    DATE;

-- Map legacy free-text types onto the standard category list (original text kept in description).
CREATE TEMP TABLE expense_type_map ON COMMIT DROP AS
SELECT DISTINCT type AS old_type,
    CASE
        WHEN type IN ('Fertilizer','Agro-chemicals','Ammonia & Latex Chemicals','Fuel','Transport','Repairs & Maintenance',
                      'Utilities','Tools & Equipment','Planting Material','Salaries & Wages','Labour','Estate Supplies',
                      'Insurance','Taxes & Licences','Professional Fees','Interest','Bank Charges','Rent & Lease','Other') THEN type
        WHEN type ILIKE '%ammonia%'                                                   THEN 'Ammonia & Latex Chemicals'
        WHEN type ILIKE '%fertili%' OR type ILIKE '%manure%'                           THEN 'Fertilizer'
        WHEN type ILIKE '%chemical%' OR type ILIKE '%agro%' OR type ILIKE '%spray%'
             OR type ILIKE '%weed%' OR type ILIKE '%pesticide%'                        THEN 'Agro-chemicals'
        WHEN type ILIKE '%fuel%' OR type ILIKE '%diesel%' OR type ILIKE '%petrol%'     THEN 'Fuel'
        WHEN type ILIKE '%logistic%' OR type ILIKE '%transport%' OR type ILIKE '%freight%'
             OR type ILIKE '%vehicle%' OR type ILIKE '%lorry%'                         THEN 'Transport'
        WHEN type ILIKE '%repair%' OR type ILIKE '%maint%' OR type ILIKE '%service%'   THEN 'Repairs & Maintenance'
        WHEN type ILIKE '%utilit%' OR type ILIKE '%electric%' OR type ILIKE '%water%'
             OR type ILIKE '%phone%' OR type ILIKE '%internet%'                        THEN 'Utilities'
        WHEN type ILIKE '%tool%' OR type ILIKE '%equip%' OR type ILIKE '%machin%'      THEN 'Tools & Equipment'
        WHEN type ILIKE '%plant%' OR type ILIKE '%seedling%' OR type ILIKE '%sapling%' THEN 'Planting Material'
        WHEN type ILIKE '%salar%' OR type ILIKE '%wage%' OR type ILIKE '%payroll%'     THEN 'Salaries & Wages'
        WHEN type ILIKE '%labo%'                                                       THEN 'Labour'
        WHEN type ILIKE '%suppl%' OR type ILIKE '%stationer%'                          THEN 'Estate Supplies'
        WHEN type ILIKE '%insur%'                                                      THEN 'Insurance'
        WHEN type ILIKE '%tax%' OR type ILIKE '%licen%'                                THEN 'Taxes & Licences'
        WHEN type ILIKE '%profession%' OR type ILIKE '%legal%' OR type ILIKE '%audit%'
             OR type ILIKE '%consult%'                                                 THEN 'Professional Fees'
        WHEN type ILIKE '%interest%'                                                   THEN 'Interest'
        WHEN type ILIKE '%bank%'                                                       THEN 'Bank Charges'
        WHEN type ILIKE '%rent%' OR type ILIKE '%lease%'                               THEN 'Rent & Lease'
        ELSE 'Other'
    END AS new_type
FROM expenses
WHERE type IS NOT NULL;

UPDATE expenses e
SET description = COALESCE(e.description, e.type),
    type = m.new_type
FROM expense_type_map m
WHERE e.type = m.old_type AND m.new_type <> m.old_type;

UPDATE expenses SET type = 'Other' WHERE type IS NULL OR btrim(type) = '';
CREATE INDEX idx_expenses_unpaid ON expenses (is_paid);

-- Cocoa & other crop harvests
CREATE TABLE crop_harvests (
    id           VARCHAR(36)    PRIMARY KEY,
    crop         VARCHAR(30)    NOT NULL,
    harvest_date DATE           NOT NULL,
    quantity     NUMERIC(12, 3) NOT NULL,
    unit         VARCHAR(20)    NOT NULL,
    field_block  VARCHAR(100),
    employee_id  VARCHAR(36)    REFERENCES employees(employee_id),
    notes        VARCHAR(500),
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT crop_harvests_crop_check CHECK (crop IN
        ('Cocoa','Pepper','Cinnamon','Coffee','Arecanut','Cloves','Other')),
    CONSTRAINT crop_harvests_unit_check CHECK (unit IN
        ('kg','g','nuts','bunches','litres','units','cubic_ft','other')),
    CONSTRAINT crop_harvests_quantity_check CHECK (quantity > 0)
);
CREATE INDEX idx_crop_harvests_date ON crop_harvests (harvest_date);
CREATE INDEX idx_crop_harvests_crop ON crop_harvests (crop);
