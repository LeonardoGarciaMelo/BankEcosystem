package br.com.PaymentAPI;

import br.com.PaymentAPI.strategy.PaymentStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * The orchestrator for payment processing.
 * <p>
 * Demonstrates the Strategy Pattern with CDI injection. Dynamically selects the correct
 * payment algorithm at runtime without if/else blocks (Open/Closed Principle).
 * </p>
 */
@ApplicationScoped
public class PaymentProcessor {

    @Inject
    Instance<PaymentStrategy> strategies;

    /**
     * Locates the appropriate strategy and executes the payment.
     *
     * @param method                  The payment method requested by the client.
     * @param amount                  The monetary value of the transaction.
     * @param destinationAccountNumber The account that will receive the funds.
     * @param sourceAccountNumber     Origin account (required for TED; null for others).
     * @throws IllegalArgumentException if no strategy supports the provided method.
     */
    public void execute(String method, BigDecimal amount, Long destinationAccountNumber, Long sourceAccountNumber) {
        strategies.stream()
                .filter(s -> s.isApplicable(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment method not supported: " + method))
                .process(amount, destinationAccountNumber, sourceAccountNumber);
    }
}