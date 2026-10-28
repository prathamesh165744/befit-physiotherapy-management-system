package com.befit.backend.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Startup step before the seeders: lets an EXISTING PostgreSQL database store the BRANCH_MANAGER role.
 *
 * Hibernate creates "users.role" with a CHECK constraint listing the enum values at the time the table was created, and
 * ddl-auto=update never changes it - so saving a BRANCH_MANAGER would fail on a database created before this role existed.
 * If such a constraint is there and does not yet allow BRANCH_MANAGER it is replaced by one that does. Idempotent; when the
 * constraint is missing (or the database is not PostgreSQL) nothing happens. Never deletes data.
 */
@Component
@Order(1)
public class RoleConstraintMigrator implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RoleConstraintMigrator.class);
    private static final String CONSTRAINT = "users_role_check";

    @PersistenceContext private EntityManager em;
    private final TransactionTemplate tx;

    public RoleConstraintMigrator(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        // Own transaction, failures caught OUTSIDE it: a rolled-back check must never stop the application from starting.
        try {
            tx.executeWithoutResult(status -> migrate());
        } catch (RuntimeException e) {
            // Not PostgreSQL / table not there yet / no permission: leave things as they are and say why.
            log.warn("Could not check the users.role constraint ({}). If saving a BRANCH_MANAGER fails, run: "
                    + "ALTER TABLE users DROP CONSTRAINT users_role_check; ALTER TABLE users ADD CONSTRAINT users_role_check "
                    + "CHECK (role IN ('ADMIN','BRANCH_MANAGER','DOCTOR','RECEPTIONIST','PATIENT'));", e.getMessage());
        }
    }

    private void migrate() {
        {
            @SuppressWarnings("unchecked")
            List<Object> defs = em.createNativeQuery(
                    "select pg_get_constraintdef(c.oid) from pg_constraint c where c.conname = :n and c.conrelid = 'users'::regclass")
                    .setParameter("n", CONSTRAINT).getResultList();
            if (defs.isEmpty() || String.valueOf(defs.get(0)).contains("BRANCH_MANAGER")) return;
            em.createNativeQuery("alter table users drop constraint " + CONSTRAINT).executeUpdate();
            em.createNativeQuery("alter table users add constraint " + CONSTRAINT
                    + " check (role in ('ADMIN','BRANCH_MANAGER','DOCTOR','RECEPTIONIST','PATIENT'))").executeUpdate();
            log.info("users.role constraint updated to allow BRANCH_MANAGER");
        }
    }
}
