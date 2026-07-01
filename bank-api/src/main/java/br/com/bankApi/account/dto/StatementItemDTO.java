package br.com.bankApi.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for an individual line item on the bank statement..
 */
public record StatementItemDTO(
        UUID transactionId,
        LocalDateTime date,
        String type,
        BigDecimal amount,
        String description,
        BalanceEffect effect
) {}