package br.com.PaymentAPI.strategy;

import java.math.BigDecimal;

/**
 * Strategy Pattern interface for payment method implementations.
 * <p>
 * Each payment method (PIX, CREDIT_CARD, BOLETO, TED) provides its own
 * implementation with specific business rules (fees, timing, routing).
 * </p>
 */
public interface PaymentStrategy {

    /**
     * Executes the payment logic for a specific method.
     *
     * @param amount                  The monetary value of the transaction.
     * @param destinationAccountNumber The account that will receive the funds.
     * @param sourceAccountNumber     The account that originates the funds (required for TED; null otherwise).
     */
    void process(BigDecimal amount, Long destinationAccountNumber, Long sourceAccountNumber);

    /**
     * Determines whether this strategy handles the given payment method.
     *
     * @param method The payment method string from the request (e.g., "PIX").
     * @return true if this strategy handles the method.
     */
    boolean isApplicable(String method);
}