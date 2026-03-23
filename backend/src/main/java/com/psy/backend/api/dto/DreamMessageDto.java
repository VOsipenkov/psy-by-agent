package com.psy.backend.api.dto;

import java.time.Instant;

public record DreamMessageDto(
        Long id,
        String sender,
        String content,
        Instant createdAt
) {
}
