package br.com.PaymentAPI.dto;

import java.math.BigDecimal;

public record PaymentResponseDTO(
        int statusCode,
        String message,
        String method,
        BigDecimal amount
) {
}
