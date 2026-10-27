package com.aireck.javapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentProfileRequest(
    @NotBlank @Size(max = 50) String studentCode, @NotBlank @Size(max = 100) String major) {}
