# Estate ERP — Final Architecture & API Specification

## Architecture Overview

Since only ~10 users will access this system (dashboard admins + field supervisors on mobile), a **single Spring Boot monolith** is the right call. No need for microservices overhead at this scale.

```mermaid
graph TB
    subgraph Clients
        REACT["React Dashboard<br/>(estate-dashboard-react)"]
        FLUTTER["Flutter Mobile App<br/>(estate-field-app-flutter)"]
    end

    subgraph Backend ["Java Spring Boot Monolith"]
        direction TB
        NGINX["Nginx Reverse Proxy<br/>(SSL termination, static files)"]
        API["REST API Layer<br/>(Controllers + DTOs)"]
        SEC["Spring Security + JWT"]
        SVC["Service Layer<br/>(Business Logic)"]
        REPO["JPA Repositories"]
    end

    subgraph Data
        PG[(PostgreSQL)]
    end

    REACT -- HTTPS --> NGINX
    FLUTTER -- HTTPS --> NGINX
    NGINX --> API
    API --> SEC
    SEC --> SVC
    SVC --> REPO
    REPO --> PG
```

### Why a Monolith?
- **10 users** → zero need for horizontal scaling or message brokers yet.
- **Single deploy** → one `docker-compose up` spins up Nginx + Spring Boot + Postgres.
- **Simpler debugging** → no distributed tracing needed.
- **Easy to split later** → clean service-layer separation means you can extract a microservice if the estate grows.

> [!NOTE]
> The Go/Python services from your original plan remain valid for `Phase 2` of the product (IoT + AI). When you're ready, the Spring Boot app can publish events to RabbitMQ and those services subscribe.

---

## API Endpoint Specification

Base URL: `/api/v1`

All endpoints return JSON. Authenticated via `Authorization: Bearer <jwt>` header (except login/register).

---

### 1. Authentication (`/api/v1/auth`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/auth/login` | Login → returns JWT token |
| `POST` | `/auth/register` | Register new user (admin-only) |
| `GET`  | `/auth/me` | Get current user profile |
| `PUT`  | `/auth/change-password` | Change own password |

---

### 2. Employees (`/api/v1/employees`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/employees` | List all employees (paginated) |
| `GET`    | `/employees/{id}` | Get employee details |
| `POST`   | `/employees` | Create new employee |
| `PUT`    | `/employees/{id}` | Update employee info |
| `DELETE` | `/employees/{id}` | Soft-delete employee |
| `GET`    | `/employees/{id}/loans` | List loans for an employee |
| `GET`    | `/employees/{id}/transactions` | List transactions for an employee |
| `GET`    | `/employees/{id}/attendance` | Attendance history for employee |

---

### 3. Employee Loans (`/api/v1/employee-loans`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/employee-loans` | List all active loans |
| `POST`   | `/employee-loans` | Issue a new loan to an employee |
| `GET`    | `/employee-loans/{id}` | Get loan details |
| `PUT`    | `/employee-loans/{id}` | Update loan (e.g., mark inactive) |

---

### 4. Attendance (`/api/v1/attendance`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/attendance` | List attendance (filterable by date range, employee) |
| `POST`   | `/attendance` | Record attendance entry |
| `POST`   | `/attendance/bulk` | Bulk record daily attendance for all workers |
| `PUT`    | `/attendance/{id}` | Update an attendance record |
| `DELETE` | `/attendance/{id}` | Delete an attendance record |

---

### 5. Loads (`/api/v1/loads`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/loads` | List all loads (filterable by date) |
| `POST`   | `/loads` | Start a new load |
| `GET`    | `/loads/{id}` | Get load details (with latex records, metrolac, rubber solid) |
| `PUT`    | `/loads/{id}` | Update load (e.g., set end date) |
| `DELETE` | `/loads/{id}` | Delete a load |

---

### 6. Latex Records (`/api/v1/latex-records`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/latex-records` | List all latex records (filterable by load, employee) |
| `POST`   | `/latex-records` | Record a latex collection entry |
| `PUT`    | `/latex-records/{id}` | Update a latex record |
| `DELETE` | `/latex-records/{id}` | Delete a record |

---

### 7. Metrolac Readings (`/api/v1/metrolac-readings`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/metrolac-readings` | List readings (filterable by load) |
| `POST`   | `/metrolac-readings` | Add a new metrolac reading |
| `PUT`    | `/metrolac-readings/{id}` | Update a reading |

---

### 8. Ammonia Records (`/api/v1/ammonia-records`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/ammonia-records` | List ammonia records |
| `POST`   | `/ammonia-records` | Record ammonia refill or usage |

---

### 9. Rubber Solid Records (`/api/v1/rubber-solid-records`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/rubber-solid-records` | List rubber solid records |
| `POST`   | `/rubber-solid-records` | Add a rubber solid weight record |

---

### 10. Monetary Assets (`/api/v1/monetary-assets`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/monetary-assets` | List all asset transactions |
| `GET`    | `/monetary-assets/balances` | Current balance per asset type (Cash, BOC, Peoples, Seylan) |
| `POST`   | `/monetary-assets` | Record a money-in or money-out transaction |

---

### 11. Estate Loans (`/api/v1/estate-loans`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/estate-loans` | List all estate loan transactions |
| `GET`    | `/estate-loans/balances` | Current balance per loan type |
| `POST`   | `/estate-loans` | Record a loan transaction |

---

### 12. Sales — Latex (`/api/v1/sales/latex`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/sales/latex` | List all latex sales |
| `POST`   | `/sales/latex` | Record a new latex sale |
| `PUT`    | `/sales/latex/{id}` | Update sale (e.g., mark payment received) |
| `PUT`    | `/sales/latex/{id}/receive-payment` | Mark payment received and link to a monetary asset transaction |

---

### 13. Expenses (`/api/v1/expenses`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/expenses` | List all expenses (filterable by type, date range) |
| `POST`   | `/expenses` | Record an expense (auto-links to asset or estate loan) |
| `GET`    | `/expenses/{id}` | Get expense details |

---

### 14. Employee Transactions (`/api/v1/employee-transactions`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/employee-transactions` | List all (filterable by employee, type, date) |
| `POST`   | `/employee-transactions` | Record a transaction (advance, manual labor, loan payment, latex tap) |

---

### 15. Dashboard / Reports (`/api/v1/dashboard`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET`    | `/dashboard/summary` | Overall estate summary (cash, outstanding loans, today's collection) |
| `GET`    | `/dashboard/daily-report?date=` | Full daily operations report |
| `GET`    | `/dashboard/monthly-payroll?month=&year=` | Monthly payroll summary for all employees |
| `GET`    | `/dashboard/export/payroll?month=&year=&format=pdf` | Download payroll as PDF/Excel |

---

## Implementation Phases (Revised)

| Phase | What | Priority |
|-------|------|----------|
| **1** | Project scaffold (Maven, Spring Boot 3, Postgres, Flyway migration) | 🟢 Now |
| **2** | All JPA Entities + Repositories | 🟢 Now |
| **3** | Service layer (Workforce, Daily Ops, Finance, Payroll) | 🟢 Now |
| **4** | REST Controllers + DTOs (all endpoints above) | 🟢 Now |
| **5** | Spring Security + JWT auth | 🟡 Next |
| **6** | Dashboard/Reports endpoints + PDF export | 🟡 Next |
| **7** | Swagger/OpenAPI docs | 🟡 Next |
| **Future** | Go IoT ingestion, Python AI, RabbitMQ integration | 🔴 Later |

## Verification Plan

### Automated Tests
- JUnit 5 + Mockito unit tests for `FinanceService` and `PayrollService`.
- Integration tests with Testcontainers (Postgres) for full CRUD flows.

### Manual Verification
- Start locally with `./mvnw spring-boot:run`.
- Use **Swagger UI** at `http://localhost:8080/swagger-ui.html` to manually test all endpoints.
