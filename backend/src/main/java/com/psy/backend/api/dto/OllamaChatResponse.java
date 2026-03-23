package com.psy.backend.api.dto;

import java.util.Map;

public record OllamaChatResponse(
        Map<String, String> message
) {
}
