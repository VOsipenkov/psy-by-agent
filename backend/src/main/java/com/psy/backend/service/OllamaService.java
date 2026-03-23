package com.psy.backend.service;

import com.psy.backend.api.dto.OllamaChatRequest;
import com.psy.backend.api.dto.OllamaChatResponse;
import com.psy.backend.domain.DreamMessageEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OllamaService {

    private final RestClient ollamaClient;
    private final String model;

    public OllamaService(RestClient ollamaRestClient, @Value("${app.ollama.model}") String model) {
        this.ollamaClient = ollamaRestClient;
        this.model = model;
    }

    public String askAssistant(List<DreamMessageEntity> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", """
                Ты ассистент по интерпретации снов.
                Давай мягкую психологическую интерпретацию и вероятные значения символов сна.
                Не выдавай категоричные медицинские диагнозы и не утверждай абсолютную истину.
                """);
        messages.add(system);

        for (DreamMessageEntity item : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role", item.getSender().name().equals("USER") ? "user" : "assistant");
            m.put("content", item.getContent());
            messages.add(m);
        }

        OllamaChatResponse response;
        try {
            response = ollamaClient.post()
                    .uri("/api/chat")
                    .body(new OllamaChatRequest(model, messages, false))
                    .retrieve()
                    .body(OllamaChatResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Ollama недоступна. Проверь, что сервис запущен локально.", e);
        }

        if (response == null || response.message() == null) {
            throw new IllegalStateException("Пустой ответ от Ollama");
        }
        return response.message().getOrDefault("content", "Не удалось получить ответ от модели.");
    }

    public String summarizeConversation(List<DreamMessageEntity> history) {
        List<DreamMessageEntity> enrichedHistory = new ArrayList<>(history);
        DreamMessageEntity summaryPrompt = new DreamMessageEntity();
        summaryPrompt.setSender(com.psy.backend.domain.MessageSender.USER);
        summaryPrompt.setContent("Сделай краткую итоговую интерпретацию этого сна на основе всей переписки.");
        enrichedHistory.add(summaryPrompt);
        return askAssistant(enrichedHistory);
    }
}
