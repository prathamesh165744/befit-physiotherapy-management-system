# BeFit – multi-branch, billing and "View File" upgrade

## 1. Root cause of "Failed to load profile. Please log in again."

Found by tracing the code (not guessed):

1. `frontend/src/App.jsx` mapped **`/staff-dashboard/patients/:id` → `PatientProfile`**.
2. `PatientProfile.jsx` is the *patient's own* "My Medical Profile" page. It never reads `:id`; it calls **`GET /api/patients/me`**.
3. The "View File" link in `PatientsDirectory.jsx` therefore sent the **receptionist's** token to `/me`. `PatientController.getMyProfile` looks the patient up by the token's email → a staff member has no `patients` row → `RuntimeException`.
4. The controller's `catch (Exception)` turned *every* failure into **HTTP 401**, and the page printed one fixed message for any error – "Please log in again".

So the login/JWT/CORS were not the problem: wrong component + a catch-all that mislabelled a "not found" as "not authenticated".

**Fix:** a new staff page `StaffPatientProfile.jsx` that calls the new **`GET /api/patients/{id}`** (branch-checked on the server) and handles 401 / 403 / 404 / 5xx / network separately; the patient's own page moved to `/dashboard/profile`; the backend now returns real status codes (`GlobalExceptionHandler`). Only a genuine 401 sends the user to login (`api.js`).

Two related problems found on the way (also fixed):
* `SecurityConfig` was `anyRequest().permitAll()` with no JWT filter – **nothing was authenticated** and any caller could pass any `branchId`. Real JWT auth + role rules are now enforced.
* `JwtUtil` generated a **new random signing key on every server start**, so every restart invalidated all logins. The key now comes from `jwt.secret`.

## 2. Security finding – please act on this

`application.properties` in the uploaded project contained the **live database URL, username and password in plain text**. The new version reads them from environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). Because that file was in the zip you shared (and probably in Git history), **rotate the database password** and make sure the old value is not in any public repository.

## 3. Conflicts with the brief, and what I did

| Brief | Reality in the code | Decision |
|---|---|---|
| MySQL | The project uses **PostgreSQL** (Neon) | Kept PostgreSQL. Migration SQL is PostgreSQL. |
| Stories 5, 8 | Appointment lifecycle and payment method/status were only partly there (every bill was created as "PAID" with no method) | Implemented lifecycle rules (`AppointmentService`) and real payments (method, reference, partial payments, status UNPAID/PARTIAL/PAID/CANCELLED). |
| Story 4 (available slots), Story 9 (bill delivery by email/WhatsApp) | **Not present in the uploaded code** | Not implemented (needs provider credentials / scope). Added a doctor double-booking check only. |
| "Edit paid invoices only if rules allow" | No existing rule | PAID invoices are **locked**; only an **ADMIN** can apply a *controlled correction* (reason required, fully audited). Receptionists can edit UNPAID/PARTIAL invoices. |
| "Branch approval if required" | Not specified | `befit.transfer.approval-required=true` (default): the **receiving** branch approves. Set `false` for the old immediate behaviour. |

## 4. What was built

**Billing (receptionist dashboard)**
* **Print one invoice** – `Print` button per row opens an A4 preview of *that* invoice only (invoice no., patient, branch, appointment, itemised lines, discount, tax, total, paid, balance, payment method/status, date). Printing hides the whole app via print CSS (`index.css`, `PrintPortal.jsx`).
* **Print All** – calls `GET /api/billing/report` which returns **every record matching the current search/status/date filters across all pages** (cap 5 000, with a visible warning if exceeded). Titled "Billing Report", with filter summary and totals – clearly different from an invoice.
* **Export to Excel** – `GET /api/billing/export` → real `.xlsx` (Apache POI): numeric money cells, real date cells, totals row, auto-filter, "Report Info" sheet. Same filters as the list. Refuses (422) instead of silently truncating above 20 000 rows.
* **Edit** – items / quantity / price / discount / tax %, totals recalculated on the **server** (`BillCalculator`), reason required, optimistic revision check, cannot drop below the amount already paid, payments never removed. Full **audit trail** (`bill_audit_log`: who, when, reason, before/after JSON). Admin-only **Cancel** (invoice is kept, marked CANCELLED).
* **Payments** – `POST /api/billing/{id}/payments`, row-locked and **idempotent** (`idempotencyKey` unique) so a double-click/retry cannot record twice; cannot exceed the balance.
* The billing list is server-paged with search / status / date filters and sortable columns.

**Multi-branch**
* `branch_id` is on appointments, invoices, payments and patients; indexes added for the real query patterns (branch+date, branch+status, patient, doctor+date).
* Branch rules are enforced **on the server** in one class (`BranchAccess`): receptionists/doctors only ever see their own branch (a `?branchId=` from the browser is ignored); admins can pick a branch or "All". Records of other branches return **403**.
* A new `Branch` endpoint (`POST/PUT /api/branches`) lets an admin add branches without code changes; registration reads the branch list from the API.

**Patient transfer** (`TransferService`)
* Patient identity never changes. A transfer changes only `patients.branch_id` and writes `patient_branch_history` + a `branch_transfer_requests` row (who requested, who approved, reason, dates).
* **Appointments, invoices and payments are never touched** – they keep the `branch_id` they were created with. New appointments are booked at the patient's *current* branch. A transfer is refused while the patient has an open appointment.
* Old branch staff can still open the file and see **their own** branch's records; admins and the patient see everything with branch labels.

## 5. Database changes (additive only – nothing dropped)

* New tables: `bill_items`, `payments`, `bill_audit_log`, `branch_transfer_requests`.
* New columns: `branches` (address, phone, email); `patient_branch_history` (authorized_by, reason); `bills` (invoice_number, subtotal, discount_amount, tax_percent, tax_amount, total_amount, amount_paid, payment_method, revision, created_by, updated_by, updated_at). The old `bills.amount` column is **kept** and mirrored.
* Old invoices are converted on startup by `DataFixer` (each becomes 1 line item + 1 payment marked `MIGRATED`, so totals reconcile). It is idempotent.
* **Migration:** `ddl-auto=update` creates everything automatically. If you prefer to apply SQL yourself (recommended for production) use `docs/migrations/V2__multi_branch_billing.sql` – it also contains the **foreign keys and the "one live invoice per appointment" / "one pending transfer per patient" unique indexes**, preceded by queries that must return no rows. Take a backup first.

## 6. API changes

| Endpoint | Change |
|---|---|
| everything except `/api/auth/**`, `GET /api/branches` | **now needs a valid JWT** and the right role; errors are JSON `{status, code, message}` |
| `POST /api/auth/login` | success body unchanged; failures are **401** `INVALID_CREDENTIALS` (was 400, and revealed whether the email exists) |
| `GET /api/patients` | **paged** `{items,page,size,totalItems,totalPages}`; params `q, scope(current/all), page, size, sort, dir` |
| `GET /api/patients/{id}` | **new** – staff "View File" |
| `GET /api/patients/{id}/history`, `GET /api/patients/options`, `POST /api/patients/{id}/transfer` (admin) | new |
| `POST /api/patients` | now a validated DTO; branch decided by the server |
| `GET/PUT /api/patients/me`, `GET /me/history, /me/transfers`, `DELETE /me/branch-request` | `/me` returns a DTO (never the login row); 404 `PROFILE_NOT_FOUND` instead of 401 |
| `PUT /api/patients/me/branch` | now creates a **PENDING request** (or applies immediately if approval is off); returns the request |
| `GET /api/appointments` | **paged**; params `date, statuses, page, size, dir` |
| `POST /api/appointments` | validated DTO, `doctorId` required, booked at the patient's current branch, 409 if the doctor's slot is taken |
| `PUT /api/appointments/{id}/status` | lifecycle rules enforced (409 on an illegal change) |
| `GET /api/billing` | **paged** + filters `q,status,from,to,page,size,sort,dir` |
| `GET /api/billing/{id}`, `/{id}/audit`, `/report`, `/export` | new |
| `PUT /api/billing/{id}`, `POST /{id}/payments`, `POST /{id}/cancel` | new |
| `POST /api/billing/manual`, `/appointment/{id}` | accept `items[]`, `discountAmount`, `taxPercent`, `paymentMethod`, `payNow`; legacy `{amount, description, patientName}` still works. 409 if the appointment is already billed |
| `GET /api/transfers`, `POST /{id}/approve|reject` | new (receiving branch / admin) |
| `GET /api/branches` (public), `/all`, `POST`, `PUT /{id}` (admin) | new |
| `GET /api/staff/doctors` | new – branch doctors for the booking form |
| `GET /api/dashboard/staff-data` | adds `branchName`, `branchAddress`; branch comes from the login |

## 7. Files

**Created (backend):** `config/DemoDataSeeder`; `controller/{Branch,Staff,Transfer}Controller`; `dto/*` (AppointmentCreateRequest, AppointmentDTO, BillAuditDTO, BillDTO, BillDetailDTO, BillReportDTO, BillRequests, BranchDTO, BranchRequest, DecisionBody, DoctorDTO, PageResponse, PatientHistoryDTO, PatientProfileDTO, PatientRegistrationRequest, PatientSummaryDTO, TransferDTO, TransferRequestBody); `entity/{BillAuditLog,BillItem,BillStatus,BranchTransferRequest,Payment,PaymentMethod,PaymentStatus,TransferStatus}`; `exception/{ApiError,ApiException,GlobalExceptionHandler}`; `repository/{BillAuditLog,BillItem,BranchTransferRequest,Payment}Repository`; `security/{AuthUser,BranchAccess,JwtAuthFilter,RestAccessDeniedHandler,RestAuthEntryPoint}`; `service/{BillCalculator,BillExcelExporter,BillingService,BranchService,TransferService}`; `util/JsonUtil`; tests `BillCalculatorTest`, `BillExcelExporterTest`.
**Created (frontend):** `api.js`, `utils/format.js`, `components/PrintPortal.jsx`, `components/billing/{InvoiceDocument,InvoicePreviewModal,BillReportModal,EditBillModal,PaymentModal}.jsx`, `pages/{StaffPatientProfile,TransferRequests}.jsx`, `.env.example`.
**Created (other):** `docs/CHANGES.md`, `docs/migrations/V2__multi_branch_billing.sql`, `.gitignore`.
**Modified:** `backend/pom.xml` (+validation starter, +Apache POI 5.4.1), `application.properties`, `BranchSeeder`, `DataFixer`, `DataSeeder`, `SecurityConfig`, `AuthController`, `AppointmentController`, `BillingController`, `DashboardController`, `PatientController`, `DashboardDTO`, `PatientProfileUpdate`, `RegisterRequest`, entities `Appointment, Bill, Branch, PatientBranchHistory`, repositories `Appointment, Bill, Branch, PatientBranchHistory, Patient, User`, services `Appointment, Dashboard, Patient, User`, `JwtUtil`; frontend `App, main, index.css, auth/Login, auth/Register, layouts/StaffLayout, pages/{AppointmentsList,Billing,History,PatientDashboard,PatientProfile,PatientsDirectory,StaffDashboard}, components/modals/{GenerateBill,ManualBill,NewAppointment,RegisterPatient}Modal`, `README.md`.
**Removed:** `dto/AppointmentRequest.java` (unused).

Behaviour changes you may notice: gender is stored as Male/Female/Other (was "M/F/O" from one form); passwords on registration must be ≥ 8 characters; `History` is now a real page (it was a byte-for-byte copy of Appointments).

## 8. Testing – what was and was not verified

I could not run Maven or a database in my environment (no access to Maven Central / PostgreSQL), so be clear about the evidence:

| Check | Result |
|---|---|
| Frontend `npm run build` (Vite) | ✅ passes |
| Frontend `npm run lint` | ⚠️ 40 problems vs 32 in your original; all are the same two rule families that already fail (`React` unused / setState-in-effect). No new real defects. |
| Backend compiles | ✅ all 80+ main classes compile with `javac` **against stub versions** of Spring/JPA/JWT/servlet APIs and the real POI API. This catches typos and type errors in my code but is **not** the real Spring Boot 4.1.1 classpath – run `./mvnw -q compile` first. |
| `BillCalculator` unit tests (5) and legacy-bill mapping tests (2) | ✅ 7 pass (really executed) |
| Excel export tests (2) | ❌/⚠️ **not run** – the POI jar available to me was stripped of its OOXML schema classes (`NoClassDefFoundError`), so XSSF cannot run here. The code uses only standard POI calls; the tests are included – please run `./mvnw test`. |
| JPQL queries, Spring wiring, security rules, seeding, migration SQL | ⚠️ **not executed** against PostgreSQL. Written to avoid known pitfalls (no null query parameters, explicit count queries, row locks), but first start is the real test. |
| Browser printing / print preview layout | ⚠️ **not visually tested** (no browser). Built on a single-render print portal + `@media print`; please check the preview and "Save as PDF" once. |

### Suggested acceptance walk-through (uses the seeded data)
1. Log in `reception@befit.com` → Billing: 24+ invoices, 10 per page. Filter, then **Print All** (all pages) and **Export to Excel** (open the file; amounts/dates are real numbers/dates). **Print** one row → only that invoice.
2. **Edit** an UNPAID invoice → change a quantity, give a reason → totals update, history shows before/after. Try editing a PAID one → locked; log in as `admin@befit.com` → controlled correction.
3. **Pay** a PARTIAL invoice twice quickly → one payment only.
4. Log in `reception.baner@befit.com` → cannot open Karve Road invoices (403) and sees only Baner data.
5. Patients → **View File** → profile loads. Open `rohan`: Baner receptionist sees Baner visits only; admin sees Karve Road invoices still labelled "Karve Road".
6. `reception.kothrud@befit.com` → Transfers → approve Meera's request → her next appointment is booked at Kothrud, her Viman Nagar invoice stays at Viman Nagar.
7. Stop the DB or use a bad id → View File shows a specific message, **not** the login screen. Let the token expire / clear it → redirected to login.

## 9. Manual steps still needed
* Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`; **rotate the exposed DB password**.
* Production: set `SEED_DEMO_DATA=false` (demo passwords are in the README), and prefer applying `V2__multi_branch_billing.sql` yourself.
* Decide whether branch transfers need approval (`befit.transfer.approval-required`).
* Story 9 (email/WhatsApp bill delivery) and appointment-slot availability (Story 4) do not exist in the uploaded code and still need building.
* No refresh-token mechanism exists in the project; an expired token sends the user to login (with a message). Adding refresh tokens is a separate piece of work.
