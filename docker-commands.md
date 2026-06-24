# Docker Cheat Sheet — Estate ERP

This guide provides a comprehensive list of Docker commands required to run, monitor, and manage the Madukotawatte Estate ERP backend services.

---

## 1. Core Lifecycle Commands

### Start the Application (Recommended)
Builds the Java application, pulls required database/Nginx images, and runs the containers in the background (detached mode):
```bash
docker compose up --build -d
```

### Stop the Application
Stops all running containers without destroying database volume data:
```bash
docker compose stop
```

### Stop and Remove Containers
Stops the application and completely removes the containers and networks:
```bash
docker compose down
```

### Reset / Wipe Database Data
Stops the containers and destroys the persistent database volume. **Caution: This wipes all your PostgreSQL data**:
```bash
docker compose down -v
```

---

## 2. Monitoring & Logs

### View Status of Containers
List all running containers, their ports, and their health check status:
```bash
docker compose ps
```

### Stream Application Logs
Follow real-time console printouts from the Spring Boot backend service:
```bash
docker compose logs -f app
```

### Stream Database Logs
Follow database engine logs (useful to check connection issues or sql syntax errors):
```bash
docker compose logs -f db
```

### View All Logs Together
Shows interleaved logs from App, Database, and Nginx:
```bash
docker compose logs -f
```

---

## 3. Database Operations (PostgreSQL)

### Open PostgreSQL Terminal (psql)
Access the running Postgres console directly to run raw SQL queries:
```bash
docker exec -it estate_erp_db psql -U hhm -d estate_erp
```
*Useful psql commands once connected:*
* `\dt` — List all tables
* `\d <table_name>` — Describe table schema
* `SELECT * FROM employees;` — Query table
* `\q` — Exit console

---

## 4. Troubleshooting & Cache Cleans

### Rebuild a Single Service
If you made changes only to the Java code and want to rebuild the backend without touching the database:
```bash
docker compose up --build -d app
```

### Fix Port Conflicts (Windows)
If you get `ports are not available` or `listen tcp 0.0.0.0:8080: bind` errors:

1. Find the PID of the process occupying the port (e.g. port `8080`):
   ```powershell
   netstat -ano | findstr :8080
   ```
2. Kill the process using the PID retrieved (e.g. if PID is `1234`):
   ```powershell
   taskkill /F /PID 1234
   ```
   *(Note: If the process is a Windows system service, you might need to change host ports in `docker-compose.yml` instead).*

### Prune Unused Docker Volumes & Images
If you run out of disk space or want to clear cached builder states:
```bash
docker system prune -a --volumes
```
