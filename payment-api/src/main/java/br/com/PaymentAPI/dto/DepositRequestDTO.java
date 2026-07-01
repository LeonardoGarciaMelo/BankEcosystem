package br.com.PaymentAPI.dto;

import java.math.BigDecimal;

public record DepositRequestDTO(
        Long destinationAccountNumber,
        BigDecimal amount
) {
}
