package com.aireck.javapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @Schema(example = "user@example.com") @NotBlank @Email @Size(max = 255) String email,
    @Schema(example = "secret1234") @NotBlank String password) {}
