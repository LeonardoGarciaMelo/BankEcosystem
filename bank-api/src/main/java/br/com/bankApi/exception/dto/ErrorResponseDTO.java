package br.com.bankApi.exception.dto;

import java.time.LocalDateTime;

/**
 * Standardized JSON structure for all API errors.
 */
public record ErrorResponseDTO(
        int status,
        String error,
        String path,
        LocalDateTime timestamp
) {}
