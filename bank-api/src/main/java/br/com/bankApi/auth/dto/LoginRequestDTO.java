package br.com.bankApi.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for the login request body.
 */
public record LoginRequestDTO(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}