package com.aireck.javapp.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record UserResponse(
    Integer id,
    String name,
    String lastname,
    String email,
    String documentType,
    String documentNumber,
    LocalDate birthDate,
    String phoneNumber,
    String address,
    boolean active,
    Set<String> roles,
    StudentProfileResponse studentProfile,
    Instant createdAt,
    Instant updatedAt) {}
