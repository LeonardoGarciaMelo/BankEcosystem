package br.com.PaymentAPI.dto;

import java.math.BigDecimal;

/**
 * DTO used to call the Bank API's transfer endpoint.
 * Field names must match the bank-api TransferRequestDTO contract exactly.
 */
public record TransferRequestDTO(
        Long originAccountNumber,
        Long destinationAccountNumber,
        BigDecimal amount
) {}