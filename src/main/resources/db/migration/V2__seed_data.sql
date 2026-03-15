-- ============================================================
-- V2__seed_data.sql
-- Madukotawatte Estate ERP - Sample Data for Dev/Testing
-- ============================================================

-- 1. Initial Employees
INSERT INTO employees (employee_id, name, joined_date, salary, position) VALUES
('emp-001', 'K. Perera', '2023-01-15', 45000.00, 'Supervisor'),
('emp-002', 'S. Gunawardena', '2023-02-10', 35000.00, 'Latex Collector'),
('emp-003', 'M. Silva', '2023-03-05', 35000.00, 'Latex Collector'),
('emp-004', 'D. Rajapaksa', '2023-05-20', 38000.00, 'Operations Assistant');

-- 2. Initial Users
-- Password for both is 'password123' (BCrypt hash)
INSERT INTO users (user_id, username, email, pw_hash, role, employee_id) VALUES
('usr-001', 'admin', 'admin@madukotawatte.com', '$2a$12$PKDV0zlPiDok7K1PwBNYn.Ut8icLrbCUY/cwwEYbyGfyifdklWKVW', 'ROLE_ADMIN', 'emp-001'),
('usr-002', 'supervisor', 'sup@madukotawatte.com', '$2a$12$7KvVvwShHP/0vVJhr43I2eqIzpPvr7ZxPaPvfcR0ZtnHz1fqIKT5q', 'ROLE_SUPERVISOR', 'emp-004');

-- 3. Monetary Asset Initial Balances
INSERT INTO monetary_asset_transactions (id, transaction_type, asset_type, last_amount, new_amount) VALUES
('initial-asset-1', 'money in', 'Bank-BOC', 0.00, 500000.00),
('initial-asset-2', 'money in', 'Cash', 0.00, 50000.00),
('initial-asset-3', 'money in', 'Bank-Seylan', 0.00, 250000.00);

-- 4. Sample Load
INSERT INTO load_table (load_id, load_type, start_date) VALUES
('load-2024-03-14', 'Latex Collection', '2024-03-14 06:00:00');

-- 5. Sample Attendance for Today
INSERT INTO attendance (attendance_id, employee_id, timestamp, no_of_trees, no_work) VALUES
('att-001', 'emp-002', '2024-03-14 06:15:00', 450, 'none'),
('att-002', 'emp-003', '2024-03-14 06:20:00', 420, 'none');

-- 6. Sample Metrolac Reading
INSERT INTO metrolac_readings (metrolac_id, load_id, temperature, timestamp) VALUES
('metro-001', 'load-2024-03-14', 28.5, '2024-03-14 09:30:00');
