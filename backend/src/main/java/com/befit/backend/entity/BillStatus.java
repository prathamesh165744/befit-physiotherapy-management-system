package com.befit.backend.entity;

/** Payment status of an invoice. Legacy rows only ever contain PAID. */
public enum BillStatus {
    UNPAID,     // issued, nothing paid yet
    PARTIAL,    // some money received, balance outstanding
    PAID,       // fully paid (locked for editing)
    CANCELLED   // voided - kept for history, never deleted
}
