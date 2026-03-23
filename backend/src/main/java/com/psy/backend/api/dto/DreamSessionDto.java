package com.psy.backend.api.dto;

import java.time.Instant;
import java.util.List;

public record DreamSessionDto(
        Long id,
        String title,
        String status,
        String finalInterpretation,
        Instant createdAt,
        Instant updatedAt,
        List<DreamMessageDto> messages
) {
}
