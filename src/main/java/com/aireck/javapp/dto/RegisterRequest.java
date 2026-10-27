package com.aireck.javapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Schema(example = "John") @NotBlank @Size(max = 100) String name,
    @Schema(example = "Doe") @NotBlank @Size(max = 100) String lastname,
    @Schema(example = "john.doe@example.com") @NotBlank @Email @Size(max = 255) String email,
    @Schema(example = "secret1234") @NotBlank @Size(min = 8, max = 72) String password,
    @NotNull @Valid StudentProfileRequest studentProfile) {}
