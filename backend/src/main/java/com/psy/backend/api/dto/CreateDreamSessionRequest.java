package com.psy.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDreamSessionRequest(
        @NotBlank @Size(max = 255) String title
) {
}
