package com.psy.backend.api.dto;

public record AuthResponse(
        String token,
        String username
) {
}
