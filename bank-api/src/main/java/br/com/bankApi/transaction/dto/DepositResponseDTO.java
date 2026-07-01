package br.com.bankApi.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for deposit receipts.
 * Custom-built to exclude the origin account, preventing null fields in the JSON response.
 */
public record DepositResponseDTO(
        UUID transactionId,
        Long destinationAccountNumber,
        BigDecimal amount,
        LocalDateTime date,
        String type
) {}