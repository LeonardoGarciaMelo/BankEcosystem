package br.com.bankApi.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Data Transfer Object for withdrawal requests.
 */
public record WithdrawalRequestDTO(
        @NotNull(message = "Origin account number is required")
        Long originAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Withdrawal amount must be greater than zero")
        BigDecimal amount
) {}
