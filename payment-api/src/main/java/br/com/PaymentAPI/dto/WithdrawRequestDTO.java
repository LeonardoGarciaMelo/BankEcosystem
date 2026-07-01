package br.com.PaymentAPI.dto;

import java.math.BigDecimal;

/**
 * DTO used to call the Bank API's withdraw endpoint.
 * Field names must match the bank-api WithdrawalRequestDTO contract exactly.
 */
public record WithdrawRequestDTO(
        Long originAccountNumber,
        BigDecimal amount
) {}