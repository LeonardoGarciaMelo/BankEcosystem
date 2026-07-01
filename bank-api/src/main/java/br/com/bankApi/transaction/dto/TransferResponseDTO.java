package br.com.bankApi.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for outgoing transaction receipts.
 * Prevents Infinite Recursion and Sensitive Data Leaks (like exposing User Credentials via Account relation).
 */
public record TransferResponseDTO(
        UUID transactionId,
        Long originAccountNumber,
        Long destinationAccountNumber,
        BigDecimal amount,
        LocalDateTime date,
        String type
) {}