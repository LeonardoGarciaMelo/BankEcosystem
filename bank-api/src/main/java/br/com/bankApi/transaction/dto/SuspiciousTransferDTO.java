package br.com.bankApi.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO that travels through the RabbitMQ queue to be analyzed by the Anti-Fraud system.
 */
public record SuspiciousTransferDTO(
        Long originAccount,
        Long destinationAccount,
        BigDecimal amount,
        LocalDateTime timestamp,
        String status
) {}