package com.pesu.ooad.healthcare.controller;

import com.pesu.ooad.healthcare.model.Bill;
import com.pesu.ooad.healthcare.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BillingController — REST Controller for the billing module.
 *
 * Endpoints (UML / requirements):
 *   POST /billing/generate/{appointmentId}  → trigger invoice generation
 *   POST /billing/pay/{billId}              → process payment
 *
 * This controller is intentionally thin: all business logic lives in
 * {@link BillingService}. The controller only:
 *   1. Parses HTTP input
 *   2. Delegates to the service
 *   3. Wraps the result in an HTTP response
 *
 * Note: The project uses Thymeleaf for HTML views (Module 3 sets the pattern).
 * The billing module exposes REST endpoints so it can be consumed by:
 *   - A Thymeleaf form page (future UI)
 *   - The H2-console / Postman for integration testing today
 */
@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;

    @Autowired
    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // -------------------------------------------------------------------------
    // POST /billing/generate/{appointmentId}
    // -------------------------------------------------------------------------

    /**
     * Triggers the billing cycle for a completed appointment.
     *
     * Activity Diagram flow triggered:
     *   "Consultation completed" → generateInvoice() → notifyUser() →
     *   Bill status: PENDING_PAYMENT
     *
     * Request body (JSON):
     * {
     *   "amount": 1500.00
     * }
     *
     * Response: the persisted Bill (JSON)
     *
     * @param appointmentId path variable — ID of the completed appointment
     * @param body          JSON body containing "amount"
     */
    @PostMapping("/generate/{appointmentId}")
    public ResponseEntity<?> generateInvoice(
            @PathVariable Long appointmentId,
            @RequestBody Map<String, Double> body) {

        try {
            Double amount = body.get("amount");
            if (amount == null || amount <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "'amount' must be a positive number."));
            }

            Bill bill = billingService.generateInvoiceForAppointment(appointmentId, amount);
            return ResponseEntity.ok(billToResponse(bill));

        } catch (IllegalStateException e) {
            // e.g., duplicate bill for same appointment
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /billing/pay/{billId}
    // -------------------------------------------------------------------------

    /**
     * Processes payment for an existing bill.
     *
     * Activity Diagram flow triggered:
     *   "Select payment method" → processPayment(strategy) →
     *   "Update status to Paid" → "Notify user (receipt)"
     *
     * Request body (JSON):
     * {
     *   "paymentMethod": "CARD"   // or "CASH"
     * }
     *
     * Response: the updated Bill with status PAID (JSON)
     *
     * @param billId the ID of the bill to pay
     * @param body   JSON body containing "paymentMethod"
     */
    @PostMapping("/pay/{billId}")
    public ResponseEntity<?> payBill(
            @PathVariable Long billId,
            @RequestBody Map<String, String> body) {

        try {
            String paymentMethod = body.get("paymentMethod");
            if (paymentMethod == null || paymentMethod.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "'paymentMethod' is required (CARD or CASH)."));
            }

            Bill bill = billingService.processPayment(billId, paymentMethod);
            return ResponseEntity.ok(billToResponse(bill));

        } catch (IllegalArgumentException e) {
            // Unknown bill ID or unknown payment method
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // Invalid state transition or declined payment
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /billing/{billId}  — helper for querying a bill's current state
    // -------------------------------------------------------------------------

    /**
     * Retrieves a bill by ID — useful for the UI to poll current status.
     *
     * @param billId the bill ID
     */
    @GetMapping("/{billId}")
    public ResponseEntity<?> getBill(@PathVariable Long billId) {
        try {
            Bill bill = billingService.getBillById(billId);
            return ResponseEntity.ok(billToResponse(bill));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------

    /**
     * Converts a Bill entity to a simple response map (avoids requiring a
     * dedicated DTO class while keeping the controller layer clean).
     */
    private Map<String, Object> billToResponse(Bill bill) {
        return Map.of(
            "id",            bill.getId(),
            "appointmentId", bill.getAppointmentId(),
            "amount",        bill.getAmount(),
            "status",        bill.getStatus().name()
        );
    }
}
