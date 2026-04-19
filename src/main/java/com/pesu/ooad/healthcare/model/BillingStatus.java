package com.pesu.ooad.healthcare.model;

/**
 * Represents the lifecycle states of a Bill as defined in the
 * UML State Machine Diagram (Page 7).
 *
 * Strict transition order (no skipping allowed):
 *   UNBILLED → INVOICE_GENERATED → PENDING_PAYMENT → PROCESSING → PAID
 */
public enum BillingStatus {
    UNBILLED,
    INVOICE_GENERATED,
    PENDING_PAYMENT,
    PROCESSING,
    PAID
}
