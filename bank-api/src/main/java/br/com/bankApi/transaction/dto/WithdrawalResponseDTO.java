package br.com.bankApi.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for withdrawal receipts.
 */
public record WithdrawalResponseDTO(
        UUID transactionId,
        Long originAccountNumber,
        BigDecimal amount,
        LocalDateTime date,
        String type
) {}