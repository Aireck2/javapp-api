package com.aireck.javapp.dto;

import java.time.Instant;

public record StudentProfileResponse(
    Integer userId, String studentCode, String major, Instant createdAt, Instant updatedAt) {}
