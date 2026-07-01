package br.com.bankApi.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Data Transfer Object for incoming transfer requests.
 * Ensures the payload is strictly validated before hitting the Service layer.
 */
public record TransferRequestDTO(

        @NotNull(message = "Origin account number is required")
        Long originAccountNumber,

        @NotNull(message = "Destination account number is required")
        Long destinationAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Transfer amount must be greater than zero")
        BigDecimal amount
) {}