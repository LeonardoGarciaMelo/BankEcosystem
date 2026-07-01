package br.com.PaymentAPI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing an incoming payment request.
 *
 * @param amount                 The monetary value of the payment.
 * @param method                 Payment method: PIX, CREDIT_CARD, BOLETO, TED.
 * @param destinationAccountNumber The account that will receive the funds (always required).
 * @param sourceAccountNumber    Origin account (required only for TED transfers).
 */
public record PaymentRequestDTO(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Payment method is required (PIX, CREDIT_CARD, BOLETO, TED)")
        String method,

        @NotNull(message = "Destination account number is required")
        Long destinationAccountNumber,

        Long sourceAccountNumber // Required for TED; optional for other methods
) {}