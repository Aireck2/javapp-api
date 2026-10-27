package com.aireck.javapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record UserCreateRequest(
    @Schema(example = "Jane") @NotBlank @Size(max = 100) String name,
    @Schema(example = "Smith") @NotBlank @Size(max = 100) String lastname,
    @Schema(example = "jane.smith@example.com") @NotBlank @Email @Size(max = 255) String email,
    @Schema(example = "secret1234") @NotBlank @Size(min = 8, max = 72) String password,
    @Schema(example = "DNI") @Size(max = 50) String documentType,
    @Schema(example = "12345678") @Size(max = 50) String documentNumber,
    @Schema(example = "2000-06-15") LocalDate birthDate,
    @Schema(example = "+1-555-0100") @Size(max = 20) String phoneNumber,
    @Schema(example = "123 Main St, Springfield") String address,
    @Schema(example = "[\"STUDENT\"]") @NotEmpty Set<String> roles,
    @Valid StudentProfileRequest studentProfile) {}
