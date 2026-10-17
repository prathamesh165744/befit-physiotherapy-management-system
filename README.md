# BeFit Physiotherapy Management System

Full-stack physiotherapy clinic management: **React (Vite) + Spring Boot + PostgreSQL**, multi-branch.

> What changed in this version, the root cause of the "View File" bug, API changes and test results: **[docs/CHANGES.md](docs/CHANGES.md)**

## Run it

**Requirements:** Java 17+, Maven (or the included `mvnw`), Node 20+, PostgreSQL.

### 1. Backend
The database password is no longer stored in the repository. Provide your connection through environment variables:

```bash
# Linux / macOS
export DB_URL='jdbc:postgresql://<host>/<database>?sslmode=require'   # or jdbc:postgresql://localhost:5432/befit
export DB_USERNAME='<user>'
export DB_PASSWORD='<password>'
export JWT_SECRET='<at least 32 random characters>'                    # keep it the same between restarts

cd backend
./mvnw spring-boot:run
```
```powershell
# Windows PowerShell
$env:DB_URL="jdbc:postgresql://<host>/<database>?sslmode=require"
$env:DB_USERNAME="<user>"; $env:DB_PASSWORD="<password>"; $env:JWT_SECRET="<32+ random chars>"
cd backend; .\mvnw.cmd spring-boot:run
```
(Alternatively put the same keys in `backend/src/main/resources/application-local.properties` – that file is git-ignored.)

On first start Hibernate adds the new tables/columns (nothing is dropped), `DataFixer` upgrades old rows, and – unless `SEED_DEMO_DATA=false` – the demo branches/staff/patients are created. The API listens on **http://localhost:8080**.

### 2. Frontend
```bash
cd frontend
npm install
npm run dev          # http://localhost:5173   (set VITE_API_BASE_URL in .env.local if the API is elsewhere)
npm run build        # production build
```

### 3. Tests
```bash
cd backend && ./mvnw test      # unit tests (invoice maths, Excel export). BackendApplicationTests needs a reachable database.
```

## Demo logins (created by `DemoDataSeeder`; turn off with `SEED_DEMO_DATA=false`)

| Role | Email | Password | Branch |
|---|---|---|---|
| Admin | `admin@befit.com` | `admin123` | all |
| Receptionist | `reception@befit.com` | `staff123` | Karve Road |
| Receptionist | `reception.baner@befit.com` | `staff123` | Baner |
| Receptionist | `reception.viman@befit.com` | `staff123` | Viman Nagar |
| Receptionist | `reception.kothrud@befit.com` | `staff123` | Kothrud |
| Doctors | `doctor@befit.com`, `dr.himanshu@`, `dr.smita@`, `dr.rohan@`, `dr.anjali@befit.com` | `doctor123` | one per branch (Karve has two) |
| Patient | `rohan@befit.com` | `patient123` | **moved Karve Road → Baner**; old visits & invoices stay at Karve Road |
| Patient | `meera@befit.com` | `patient123` | Viman Nagar, **pending request → Kothrud** (approve it as `reception.kothrud@befit.com`) |
| Patient | `anita@befit.com` | `patient123` | Karve Road |

Karve Road has 24+ invoices (UNPAID / PARTIAL / PAID) so pagination, filters, Print All and Excel export can be tried straight away.

## Configuration (`backend/src/main/resources/application.properties`)

| Property (env var) | Default | Meaning |
|---|---|---|
| `spring.datasource.*` (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) | local PostgreSQL | database |
| `jwt.secret` (`JWT_SECRET`) | dev-only value | token signing key, 32+ chars. **Set it for any real deployment.** |
| `jwt.expiration-ms` | 86400000 (24 h) | login lifetime |
| `befit.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`) | `http://localhost:5173` | browser origins allowed to call the API |
| `befit.transfer.approval-required` | `true` | `true`: a patient's branch change must be approved by the new branch; `false`: applied immediately |
| `befit.billing.report-max-rows` / `export-max-rows` | 5000 / 20000 | safety caps for Print All / Excel export |
| `befit.seed.demo-data` (`SEED_DEMO_DATA`) | `true` | create the demo data above |

## Adding a branch
As an admin: `POST /api/branches` with `{ "id": "WAKAD", "name": "Wakad, Pune", "address": "...", "phone": "...", "email": "...", "active": true }`
(or add a line to `BranchSeeder`). It appears in registration, transfers and the admin branch switcher without any code change.
