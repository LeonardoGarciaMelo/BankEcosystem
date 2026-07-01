package br.com.bankApi.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record FeeRequestDTO(
        @NotNull(message = "Origin account number is required")
        Long originAccountNumber,

        @NotNull(message = "Fee amount is required")
        @Positive(message = "Fee amount must be greater than zero")
        BigDecimal amount,

        String description
) {}