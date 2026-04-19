package com.pesu.ooad.healthcare.model;

import com.pesu.ooad.healthcare.strategy.PaymentStrategy;
import jakarta.persistence.*;

/**
 * JPA Entity representing a Bill in the healthcare system.
 *
 * Maps to the UML Class Diagram (Page 4):
 *   - attribute: amount: double
 *   - method:    generateInvoice()
 *   - method:    processPayment(PaymentStrategy)
 *
 * State transitions are enforced via {@link BillingStatus} following the
 * State Machine Diagram (Page 7).
 *
 * Relationship: one Bill belongs to one Appointment (appointmentId FK).
 */
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Foreign key linking this bill to the triggering appointment. */
    @Column(nullable = false)
    private Long appointmentId;

    /**
     * The monetary amount due — maps to the UML attribute {@code amount: double}.
     */
    @Column(nullable = false)
    private double amount;

    /**
     * Current lifecycle state of this bill.
     * Persisted as a String column for readability in H2 console.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingStatus status;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Bill() {
        this.status = BillingStatus.UNBILLED;
    }

    public Bill(Long appointmentId, double amount) {
        this.appointmentId = appointmentId;
        this.amount = amount;
        this.status = BillingStatus.UNBILLED;
    }

    // -------------------------------------------------------------------------
    // UML-mandated business methods
    // -------------------------------------------------------------------------

    /**
     * UML method: generateInvoice()
     *
     * State transition (State Machine, Page 7):
     *   UNBILLED → INVOICE_GENERATED
     *
     * @throws IllegalStateException if the bill is not in the UNBILLED state.
     */
    public void generateInvoice() {
        if (this.status != BillingStatus.UNBILLED) {
            throw new IllegalStateException(
                "generateInvoice() can only be called when status is UNBILLED. Current: " + this.status);
        }
        this.status = BillingStatus.INVOICE_GENERATED;
    }

    /**
     * Moves the bill from INVOICE_GENERATED → PENDING_PAYMENT.
     * Called after the user has been notified of the invoice (notifyUser step
     * in the Activity Diagram, Page 9).
     *
     * @throws IllegalStateException if the bill is not in the INVOICE_GENERATED state.
     */
    public void markPendingPayment() {
        if (this.status != BillingStatus.INVOICE_GENERATED) {
            throw new IllegalStateException(
                "markPendingPayment() requires status INVOICE_GENERATED. Current: " + this.status);
        }
        this.status = BillingStatus.PENDING_PAYMENT;
    }

    /**
     * Moves the bill from PENDING_PAYMENT → PROCESSING.
     * Represents payment initiation (user selects payment method).
     *
     * @throws IllegalStateException if the bill is not in the PENDING_PAYMENT state.
     */
    public void startProcessing() {
        if (this.status != BillingStatus.PENDING_PAYMENT && this.status != BillingStatus.PROCESSING) {
            throw new IllegalStateException(
                "startProcessing() requires status PENDING_PAYMENT or PROCESSING. Current: " + this.status);
        }
        this.status = BillingStatus.PROCESSING;
    }

    /**
     * UML method: processPayment(PaymentStrategy)
     *
     * State transition (State Machine, Page 7):
     *   PROCESSING → PAID  (on success)
     *
     * Uses the Strategy Pattern — delegates payment execution to the
     * injected {@link PaymentStrategy} without any if-else branching.
     *
     * Activity Diagram flow (Page 9):
     *   Select payment method → call processPayment(strategy) → update status to Paid
     *
     * @param strategy the concrete payment strategy (CreditCardPayment or CashPayment)
     * @return true if payment succeeded; false otherwise (bill remains PROCESSING)
     * @throws IllegalStateException if the bill is not in the PROCESSING state.
     */
    public boolean processPayment(PaymentStrategy strategy) {
        if (this.status != BillingStatus.PROCESSING) {
            throw new IllegalStateException(
                "processPayment() requires status PROCESSING. Current: " + this.status);
        }
        boolean success = strategy.pay(this.amount);
        if (success) {
            this.status = BillingStatus.PAID;
        } else {
            this.status = BillingStatus.PENDING_PAYMENT;
        }
        return success;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public BillingStatus getStatus() { return status; }
    public void setStatus(BillingStatus status) { this.status = status; }
}
