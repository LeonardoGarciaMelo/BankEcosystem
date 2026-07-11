package br.com.bankApi.auth.dto;

/**
 * DTO for the login response — the issued JWT and metadata about it.
 */
public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}