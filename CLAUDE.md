# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

No Maven wrapper is checked in. Use system `mvn` directly.

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run locally (requires PostgreSQL running)
mvn spring-boot:run

# Run with Docker Compose (PostgreSQL + app + nginx)
docker-compose up --build

# Run a single test class
mvn test -Dtest=ClassName

# Run all tests
mvn test
```

Environment variables (see `.env.example`): `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (64+ chars), `JWT_EXPIRATION_MS`, `SERVER_PORT`.

## Architecture

Single Spring Boot 3.2.3 / Java 21 monolith, intentionally sized for ~10 users (no microservices). Structured as:

```
controller → service → repository (Spring Data JPA) → PostgreSQL 16
                ↕
             mapper (entity ↔ DTO)
```

**Layers:**
- `controller/` — REST controllers, all under `/api/v1/`. One controller per domain entity.
- `service/` — Business logic consolidated into 4 coarse-grained services: `AuthService`, `DailyOperationsService`, `DashboardService`, `FinanceService`, `WorkforceService`. Each service may span multiple repositories.
- `dto/` — Request/response objects organized in sub-packages by domain (e.g., `dto/latex/`, `dto/employee/`). Never expose entities directly.
- `mapper/` — MapStruct-style manual mappers converting between entities and DTOs.
- `entity/` — JPA entities. `BaseEntity` provides `id`, `createdAt`, `updatedAt` via `@MappedSuperclass`.
- `security/` — JWT stateless auth. `JwtAuthenticationFilter` validates `Bearer` tokens on every request. `CustomUserDetailsService` loads from `users` table. BCrypt strength 12.
- `exception/` — `GlobalExceptionHandler` (@ControllerAdvice) catches domain exceptions (`ResourceNotFoundException`, `BadRequestException`, `DuplicateResourceException`, `InsufficientFundsException`) and returns a uniform `ApiError` response.

**Database:**
- Flyway manages schema: migrations live in `src/main/resources/db/migration/` (V1 = schema, V2 = seed data).
- Hibernate runs in `validate` mode — schema changes require a new Flyway migration file, not entity changes alone.

**Security:**
- Public endpoints: `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/api-docs/**`
- All other routes require a valid JWT Bearer token.
- Method-level security enabled (`@EnableMethodSecurity`).

**API Docs:** Swagger UI available at `/swagger-ui.html` when running.

**Reports:** Apache POI (`poi-ooxml`) is used for Excel export functionality.

**Infrastructure:** Docker Compose runs three services — `db` (PostgreSQL 16), `app` (port 8080), `nginx` (ports 80/443). Nginx proxies `/api/*`, `/swagger-ui/*`, `/api-docs/*` to the app; all other paths return 404.
