package br.com.bankApi.account.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Main DTO for the statement
 */
public record StatementResponseDTO(
        Long accountNumber,
        BigDecimal currentBalance,
        List<StatementItemDTO> history
) {}