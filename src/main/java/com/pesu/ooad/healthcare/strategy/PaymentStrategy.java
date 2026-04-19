package com.pesu.ooad.healthcare.strategy;

/**
 * Strategy interface — maps directly to the UML Interface PaymentStrategy
 * (Class Diagram, Page 4).
 *
 * UML method signature: pay(amount: double): boolean
 *
 * Concrete implementations (CreditCardPayment, CashPayment) must each provide
 * their own logic, allowing Bill.processPayment() to remain closed for
 * modification (Open/Closed Principle).
 */
public interface PaymentStrategy {

    /**
     * Executes the payment for the given amount.
     *
     * @param amount the monetary amount to charge
     * @return true if the payment was successful; false otherwise
     */
    boolean pay(double amount);
}
