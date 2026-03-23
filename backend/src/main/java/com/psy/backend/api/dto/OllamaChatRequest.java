package com.psy.backend.api.dto;

import java.util.List;
import java.util.Map;

public record OllamaChatRequest(
        String model,
        List<Map<String, String>> messages,
        boolean stream
) {
}
