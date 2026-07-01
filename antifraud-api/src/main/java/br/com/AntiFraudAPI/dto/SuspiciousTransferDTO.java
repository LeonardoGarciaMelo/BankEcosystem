package br.com.AntiFraudAPI.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SuspiciousTransferDTO(
        Long originAccount,
        Long destinationAccount,
        BigDecimal amount,
        LocalDateTime timestamp,
        String status
) {}
