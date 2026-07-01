package br.com.bankApi.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Data Transfer Object for external deposits.
 */
public record DepositRequestDTO(

        @NotNull(message = "Destination account number is required")
        Long destinationAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Deposit amount must be greater than zero")
        BigDecimal amount
) {}