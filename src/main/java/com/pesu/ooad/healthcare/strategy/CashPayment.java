package com.pesu.ooad.healthcare.strategy;

import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: CashPayment
 *
 * Maps to the UML concrete strategy class CashPayment (Class Diagram, Page 4).
 * Implements {@link PaymentStrategy#pay(double)}.
 *
 * Cash payments are always considered successful at the counter — no gateway
 * integration is needed. A real system would have a cashier confirmation step.
 *
 * Marked @Component so Spring can inject it by name ("cashPayment")
 * in BillingService without manual instantiation.
 */
@Component("cashPayment")
public class CashPayment implements PaymentStrategy {

    @Override
    public boolean pay(double amount) {
        // Simulate cash collection at the counter
        System.out.printf("💵 [CASH] Recording cash payment of ₹%.2f received at counter.%n", amount);
        System.out.printf("✅ [CASH] Payment of ₹%.2f confirmed.%n", amount);
        return true;
    }
}
