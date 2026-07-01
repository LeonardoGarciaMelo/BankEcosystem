package br.com.bankApi.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeeResponseDTO(
        UUID transactionId,
        Long originAccountNumber,
        BigDecimal amount,
        LocalDateTime date,
        String description,
        String type
) {}