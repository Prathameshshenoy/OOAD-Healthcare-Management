package com.pesu.ooad.healthcare.service;

import com.pesu.ooad.healthcare.model.Bill;
import com.pesu.ooad.healthcare.model.BillingStatus;
import com.pesu.ooad.healthcare.repository.BillRepository;
import com.pesu.ooad.healthcare.strategy.CashPayment;
import com.pesu.ooad.healthcare.strategy.CreditCardPayment;
import com.pesu.ooad.healthcare.strategy.PaymentStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BillingService — the single source of business logic for the billing module.
 *
 * Responsibilities (Activity Diagram, Page 9):
 *  1. Trigger billing after a consultation is completed
 *  2. Call Bill.generateInvoice()        → UNBILLED → INVOICE_GENERATED
 *  3. Notify user of invoice             → INVOICE_GENERATED → PENDING_PAYMENT
 *  4. Initiate payment (select strategy) → PENDING_PAYMENT → PROCESSING
 *  5. Call Bill.processPayment(strategy) → PROCESSING → PAID
 *  6. Notify user of receipt
 *
 * State transitions are enforced inside {@link Bill} (model layer).
 * The service orchestrates the sequence; it does NOT duplicate guard logic.
 *
 * Design: DIP — this class depends on the PaymentStrategy abstraction,
 * never on concrete payment classes directly.
 */
@Service
@Transactional
public class BillingService {

    private final BillRepository billRepository;
    private final NotificationFacade notificationFacade;
    private final CreditCardPayment creditCardPayment;
    private final CashPayment cashPayment;

    /**
     * Constructor injection (preferred over field injection for testability).
     * Spring resolves all beans automatically.
     */
    @Autowired
    public BillingService(BillRepository billRepository,
                          NotificationFacade notificationFacade,
                          CreditCardPayment creditCardPayment,
                          CashPayment cashPayment) {
        this.billRepository = billRepository;
        this.notificationFacade = notificationFacade;
        this.creditCardPayment = creditCardPayment;
        this.cashPayment = cashPayment;
    }

    // -------------------------------------------------------------------------
    // Step 1 & 2: Trigger billing → generateInvoice()
    // -------------------------------------------------------------------------

    /**
     * Called when a consultation is marked as completed (integration point
     * with AppointmentService / Appointment model).
     *
     * Activity Diagram steps covered:
     *   "Consultation completed" → "Trigger billing system" →
     *   "Call Bill.generateInvoice()" → "User reviews invoice"
     *
     * State transitions: UNBILLED → INVOICE_GENERATED → PENDING_PAYMENT
     *
     * @param appointmentId the ID of the completed appointment
     * @param amount        the consultation fee to bill
     * @return the persisted Bill after invoice generation
     * @throws IllegalStateException if a bill already exists for this appointment
     */
    public Bill generateInvoiceForAppointment(Long appointmentId, double amount) {
        // Guard: prevent duplicate bills for the same appointment
        boolean alreadyBilled = !billRepository.findByAppointmentId(appointmentId).isEmpty();
        if (alreadyBilled) {
            throw new IllegalStateException(
                "A bill already exists for appointment ID: " + appointmentId);
        }

        // Create bill and persist in UNBILLED state
        Bill bill = new Bill(appointmentId, amount);
        bill = billRepository.save(bill);

        // UML method: generateInvoice() — transitions UNBILLED → INVOICE_GENERATED
        bill.generateInvoice();
        bill = billRepository.save(bill);

        System.out.println("🧾 [BILLING] Invoice generated for appointment " + appointmentId
                           + " | Amount: ₹" + amount
                           + " | Status: " + bill.getStatus());

        // Notify patient — transitions INVOICE_GENERATED → PENDING_PAYMENT (State Machine)
        notificationFacade.notifyUser(
            appointmentId,
            String.format("Your invoice for appointment #%d has been generated. Amount due: ₹%.2f",
                          appointmentId, amount)
        );

        bill.markPendingPayment();
        bill = billRepository.save(bill);

        System.out.println("📋 [BILLING] Bill #" + bill.getId() + " is now PENDING_PAYMENT.");

        return bill;
    }

    // -------------------------------------------------------------------------
    // Steps 3–5: Select payment method → processPayment(strategy) → Paid
    // -------------------------------------------------------------------------

    /**
     * Orchestrates the full payment flow for a bill.
     *
     * Activity Diagram steps covered:
     *   "Select payment method" → "Call Bill.processPayment(strategy)" →
     *   "Update status to Paid" → "Notify user (receipt)"
     *
     * State transitions: PENDING_PAYMENT → PROCESSING → PAID
     *
     * @param billId        the ID of the bill to pay
     * @param paymentMethod "CARD" or "CASH" (case-insensitive)
     * @return the updated, persisted Bill with status PAID
     * @throws IllegalArgumentException if the bill does not exist
     * @throws IllegalArgumentException if an unknown payment method is supplied
     * @throws IllegalStateException    if payment is declined (strategy returns false)
     */
    public Bill processPayment(Long billId, String paymentMethod) {
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + billId));

        // Resolve strategy — Strategy Pattern in action; no if-else in Bill itself
        PaymentStrategy strategy = resolveStrategy(paymentMethod);

        // Transition: PENDING_PAYMENT → PROCESSING
        bill.startProcessing();
        bill = billRepository.save(bill);
        System.out.println("⏳ [BILLING] Bill #" + billId + " is PROCESSING via " + paymentMethod + ".");

        // UML method: processPayment(PaymentStrategy) — delegates to strategy
        // Transition on success: PROCESSING → PAID
        boolean success = bill.processPayment(strategy);
        bill = billRepository.save(bill);

        if (!success) {
            throw new IllegalStateException(
                "Payment failed for bill #" + billId + " using " + paymentMethod
                + ". Bill remains in PROCESSING state.");
        }

        System.out.println("✅ [BILLING] Bill #" + billId + " status: " + bill.getStatus());

        // Notify patient of successful payment (receipt step in Activity Diagram)
        notificationFacade.notifyUser(
            bill.getAppointmentId(),
            String.format("Payment of ₹%.2f received for bill #%d. Thank you!", bill.getAmount(), billId)
        );

        return bill;
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single Bill by its primary key.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public Bill getBillById(Long billId) {
        return billRepository.findById(billId)
            .orElseThrow(() -> new IllegalArgumentException("Bill not found with ID: " + billId));
    }

    /**
     * Returns the latest bill for a given appointment (if any).
     */
    @Transactional(readOnly = true)
    public Bill getLatestBillForAppointment(Long appointmentId) {
        return billRepository.findFirstByAppointmentIdOrderByIdDesc(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No bill found for appointment ID: " + appointmentId));
    }

    // -------------------------------------------------------------------------
    // Integration stub: called by AppointmentService after consultation
    // -------------------------------------------------------------------------

    /**
     * Integration method — this is how AppointmentService (Module 3) triggers
     * the billing system after a consultation is completed.
     *
     * Usage in AppointmentService:
     *   billingService.onConsultationCompleted(appointment.getId(), consultationFee);
     *
     * @param appointmentId completed appointment ID
     * @param amount        fee for the consultation
     * @return generated Bill
     */
    public Bill onConsultationCompleted(Long appointmentId, double amount) {
        System.out.println("🏥 [BILLING] Consultation completed for appointment #"
                           + appointmentId + ". Triggering billing...");
        return generateInvoiceForAppointment(appointmentId, amount);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the concrete PaymentStrategy from the user-supplied string.
     * This is the only place where a payment-method string is mapped to a bean —
     * keeping the mapping centralised (SRP) and the Bill class unaware of it (DIP).
     *
     * @param paymentMethod "CARD" or "CASH"
     * @return the appropriate {@link PaymentStrategy} implementation
     * @throws IllegalArgumentException for unrecognised method names
     */
    private PaymentStrategy resolveStrategy(String paymentMethod) {
        return switch (paymentMethod.toUpperCase()) {
            case "CARD" -> creditCardPayment;
            case "CASH" -> cashPayment;
            default -> throw new IllegalArgumentException(
                "Unknown payment method: '" + paymentMethod + "'. Accepted values: CARD, CASH");
        };
    }
}
