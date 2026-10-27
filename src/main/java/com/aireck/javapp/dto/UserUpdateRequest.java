package com.aireck.javapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record UserUpdateRequest(
    @Size(max = 100) String name,
    @Size(max = 100) String lastname,
    @Email @Size(max = 255) String email,
    @Size(max = 50) String documentType,
    @Size(max = 50) String documentNumber,
    LocalDate birthDate,
    @Size(max = 20) String phoneNumber,
    String address,
    Set<String> roles,
    Boolean active) {}
