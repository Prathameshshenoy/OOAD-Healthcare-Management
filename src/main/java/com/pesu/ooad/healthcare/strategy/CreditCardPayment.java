package com.pesu.ooad.healthcare.strategy;

import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: CreditCardPayment
 *
 * Maps to the UML concrete strategy class CreditCardPayment (Class Diagram, Page 4).
 * Implements {@link PaymentStrategy#pay(double)}.
 *
 * In a production system this would integrate with a payment gateway (e.g., Stripe).
 * For this OOAD project it simulates a successful credit-card charge.
 *
 * Marked @Component so Spring can inject it by name ("creditCardPayment")
 * in BillingService without manual instantiation.
 */
@Component("creditCardPayment")
public class CreditCardPayment implements PaymentStrategy {

    @Override
    public boolean pay(double amount) {
        // Simulate credit card gateway call
        System.out.printf("💳 [CREDIT CARD] Charging ₹%.2f to card on file...%n", amount);
        // In a real implementation: call gateway API, handle declines, etc.
        System.out.printf("✅ [CREDIT CARD] Payment of ₹%.2f authorised successfully.%n", amount);
        return true;
    }
}
