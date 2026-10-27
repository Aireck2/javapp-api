package com.aireck.javapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
    @Schema(example = "dGhpcyBpcyBhIHJlZnJlc2g...") String refreshToken,
    @Schema(example = "Bearer") String tokenType,
    @Schema(example = "3600") long expiresIn) {}
