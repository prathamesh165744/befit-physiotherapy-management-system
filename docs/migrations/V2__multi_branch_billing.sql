-- =============================================================================
-- BeFit Physiotherapy - V2: multi-branch, billing items/payments/audit, branch transfers
-- Database: PostgreSQL.   ADDITIVE ONLY - nothing is dropped, renamed or deleted.
--
-- You do NOT have to run this file for the app to work: with spring.jpa.hibernate.ddl-auto=update
-- Hibernate creates the same tables/columns on start, and DataFixer back-fills old rows.
-- Use this file when you prefer to apply schema changes yourself (recommended for production),
-- and to add the constraints that Hibernate's "update" mode does not create (section 3).
--
-- Order:  1) take a backup   2) run section 1 + 2   3) start the app once (DataFixer back-fills)
--         4) run the checks in section 3, then the constraints in section 4
-- =============================================================================

-- ---------------------------------------------------------------- 1. new columns on existing tables
ALTER TABLE branches ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE branches ADD COLUMN IF NOT EXISTS phone   VARCHAR(30);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS email   VARCHAR(255);

ALTER TABLE patient_branch_history ADD COLUMN IF NOT EXISTS authorized_by VARCHAR(255);
ALTER TABLE patient_branch_history ADD COLUMN IF NOT EXISTS reason        VARCHAR(500);

ALTER TABLE bills ADD COLUMN IF NOT EXISTS invoice_number  VARCHAR(80);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS subtotal        NUMERIC(12,2);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12,2);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS tax_percent     NUMERIC(5,2);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS tax_amount      NUMERIC(12,2);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS total_amount    NUMERIC(12,2);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS amount_paid     NUMERIC(12,2);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS payment_method  VARCHAR(20);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS revision        INTEGER;
ALTER TABLE bills ADD COLUMN IF NOT EXISTS created_by      VARCHAR(255);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS updated_by      VARCHAR(255);
ALTER TABLE bills ADD COLUMN IF NOT EXISTS updated_at      TIMESTAMP;
-- The old "amount" (double precision) column is KEPT; the app mirrors total_amount into it.

-- ---------------------------------------------------------------- 2. new tables
CREATE TABLE IF NOT EXISTS bill_items (
    id          BIGSERIAL PRIMARY KEY,
    bill_id     BIGINT        NOT NULL REFERENCES bills(id),
    description VARCHAR(255)  NOT NULL,
    quantity    INTEGER       NOT NULL,
    unit_price  NUMERIC(12,2) NOT NULL,
    line_total  NUMERIC(12,2) NOT NULL,
    sort_order  INTEGER
);
CREATE INDEX IF NOT EXISTS idx_bill_items_bill ON bill_items(bill_id);

CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL PRIMARY KEY,
    bill_id         BIGINT        NOT NULL REFERENCES bills(id),
    branch_id       VARCHAR(255)  NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    method          VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    payment_date    TIMESTAMP     NOT NULL,
    reference_no    VARCHAR(100),
    idempotency_key VARCHAR(100),
    received_by     VARCHAR(255),
    CONSTRAINT uk_payment_idempotency UNIQUE (idempotency_key)      -- stops duplicate payments
);
CREATE INDEX IF NOT EXISTS idx_payments_bill        ON payments(bill_id);
CREATE INDEX IF NOT EXISTS idx_payments_branch_date ON payments(branch_id, payment_date);

CREATE TABLE IF NOT EXISTS bill_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    bill_id         BIGINT       NOT NULL,
    branch_id       VARCHAR(255),
    action          VARCHAR(30)  NOT NULL,
    changed_by      VARCHAR(255) NOT NULL,
    changed_at      TIMESTAMP    NOT NULL,
    reason          VARCHAR(500),
    previous_values TEXT,
    new_values      TEXT
);
CREATE INDEX IF NOT EXISTS idx_bill_audit_bill   ON bill_audit_log(bill_id);
CREATE INDEX IF NOT EXISTS idx_bill_audit_branch ON bill_audit_log(branch_id);

CREATE TABLE IF NOT EXISTS branch_transfer_requests (
    id             BIGSERIAL PRIMARY KEY,
    patient_id     BIGINT       NOT NULL,
    from_branch_id VARCHAR(255) NOT NULL,
    to_branch_id   VARCHAR(255) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    reason         VARCHAR(500),
    requested_by   VARCHAR(255),
    requested_at   TIMESTAMP    NOT NULL,
    decided_by     VARCHAR(255),
    decided_at     TIMESTAMP,
    decision_note  VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_transfer_patient   ON branch_transfer_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_transfer_to_status ON branch_transfer_requests(to_branch_id, status);
-- At most ONE pending request per patient, enforced by the database as well as by the app:
CREATE UNIQUE INDEX IF NOT EXISTS uk_transfer_one_pending ON branch_transfer_requests(patient_id) WHERE status = 'PENDING';

-- extra indexes for the new list/filter queries
CREATE INDEX IF NOT EXISTS idx_bill_branch_status  ON bills(branch_id, status);
CREATE INDEX IF NOT EXISTS idx_appt_branch_status  ON appointments(branch_id, status);
CREATE INDEX IF NOT EXISTS idx_appt_doctor_date    ON appointments(doctor_id, appointment_date);
CREATE UNIQUE INDEX IF NOT EXISTS uk_bill_invoice_number ON bills(invoice_number);

-- ---------------------------------------------------------------- 3. checks to run AFTER the app has started once (all should return 0 rows)
-- (a) rows pointing at a branch that does not exist - must be empty before adding the foreign keys in section 4
SELECT 'patients' AS tbl, id FROM patients      WHERE branch_id IS NOT NULL AND branch_id NOT IN (SELECT id FROM branches)
UNION ALL SELECT 'appointments', id FROM appointments WHERE branch_id IS NOT NULL AND branch_id NOT IN (SELECT id FROM branches)
UNION ALL SELECT 'bills', id FROM bills          WHERE branch_id IS NOT NULL AND branch_id NOT IN (SELECT id FROM branches)
UNION ALL SELECT 'users', id FROM users          WHERE branch_id IS NOT NULL AND branch_id NOT IN (SELECT id FROM branches);
-- (b) invoices whose paid amount does not equal the sum of their completed payments
SELECT b.id, b.amount_paid, COALESCE(SUM(p.amount),0) AS payments_sum
FROM bills b LEFT JOIN payments p ON p.bill_id = b.id AND p.status = 'COMPLETED'
GROUP BY b.id, b.amount_paid HAVING b.amount_paid <> COALESCE(SUM(p.amount),0);
-- (c) appointments billed more than once (must be empty before the unique index below)
SELECT appointment_id, COUNT(*) FROM bills WHERE appointment_id IS NOT NULL AND status <> 'CANCELLED' GROUP BY appointment_id HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------- 4. constraints (run only when section 3 returned no rows)
ALTER TABLE patients      ADD CONSTRAINT fk_patients_branch      FOREIGN KEY (branch_id) REFERENCES branches(id);
ALTER TABLE appointments  ADD CONSTRAINT fk_appointments_branch  FOREIGN KEY (branch_id) REFERENCES branches(id);
ALTER TABLE bills         ADD CONSTRAINT fk_bills_branch         FOREIGN KEY (branch_id) REFERENCES branches(id);
ALTER TABLE users         ADD CONSTRAINT fk_users_branch         FOREIGN KEY (branch_id) REFERENCES branches(id);
ALTER TABLE branch_transfer_requests ADD CONSTRAINT fk_transfer_from FOREIGN KEY (from_branch_id) REFERENCES branches(id);
ALTER TABLE branch_transfer_requests ADD CONSTRAINT fk_transfer_to   FOREIGN KEY (to_branch_id)   REFERENCES branches(id);
-- one live invoice per appointment (race-proof duplicate-billing guard):
CREATE UNIQUE INDEX IF NOT EXISTS uk_bill_one_per_appointment ON bills(appointment_id) WHERE appointment_id IS NOT NULL AND status <> 'CANCELLED';

-- ---------------------------------------------------------------- rollback (only if needed; new tables hold new data)
-- DROP TABLE IF EXISTS branch_transfer_requests, bill_audit_log, payments, bill_items;
-- (the added columns are harmless to leave in place)
