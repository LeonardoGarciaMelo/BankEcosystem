package br.com.bankApi.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponseDTO(
        UUID refundTransactionId,
        UUID originalTransactionId,
        BigDecimal amount,
        LocalDateTime date,
        String type
) {}