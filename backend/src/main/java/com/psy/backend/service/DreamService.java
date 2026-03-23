package com.psy.backend.service;

import com.psy.backend.api.dto.DreamMessageDto;
import com.psy.backend.api.dto.DreamSessionDto;
import com.psy.backend.domain.DreamMessageEntity;
import com.psy.backend.domain.DreamSessionEntity;
import com.psy.backend.domain.MessageSender;
import com.psy.backend.domain.SessionStatus;
import com.psy.backend.repository.DreamMessageRepository;
import com.psy.backend.repository.DreamSessionRepository;
import com.psy.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class DreamService {

    private final DreamSessionRepository dreamSessionRepository;
    private final DreamMessageRepository dreamMessageRepository;
    private final UserRepository userRepository;
    private final OllamaService ollamaService;

    public DreamService(
            DreamSessionRepository dreamSessionRepository,
            DreamMessageRepository dreamMessageRepository,
            UserRepository userRepository,
            OllamaService ollamaService
    ) {
        this.dreamSessionRepository = dreamSessionRepository;
        this.dreamMessageRepository = dreamMessageRepository;
        this.userRepository = userRepository;
        this.ollamaService = ollamaService;
    }

    public List<DreamSessionDto> listSessions(Authentication auth) {
        var user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return dreamSessionRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                .map(session -> toDto(session, List.of()))
                .toList();
    }

    @Transactional
    public DreamSessionDto createSession(Authentication auth, String title) {
        var user = userRepository.findByUsername(auth.getName()).orElseThrow();
        DreamSessionEntity session = new DreamSessionEntity();
        session.setUser(user);
        session.setTitle(title);
        session.setStatus(SessionStatus.ACTIVE);
        dreamSessionRepository.save(session);
        return toDto(session, List.of());
    }

    public DreamSessionDto getSession(Authentication auth, Long sessionId) {
        DreamSessionEntity session = ownedSession(auth, sessionId);
        var messages = dreamMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return toDto(session, toMessageDtos(messages));
    }

    @Transactional
    public DreamSessionDto sendMessage(Authentication auth, Long sessionId, String content) {
        DreamSessionEntity session = ownedSession(auth, sessionId);
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Сессия уже завершена");
        }

        DreamMessageEntity userMessage = new DreamMessageEntity();
        userMessage.setSession(session);
        userMessage.setSender(MessageSender.USER);
        userMessage.setContent(content);
        dreamMessageRepository.save(userMessage);

        List<DreamMessageEntity> history = dreamMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        String response = ollamaService.askAssistant(history);

        DreamMessageEntity assistantMessage = new DreamMessageEntity();
        assistantMessage.setSession(session);
        assistantMessage.setSender(MessageSender.ASSISTANT);
        assistantMessage.setContent(response);
        dreamMessageRepository.save(assistantMessage);

        List<DreamMessageEntity> allMessages = dreamMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return toDto(session, toMessageDtos(allMessages));
    }

    @Transactional
    public DreamSessionDto completeSession(Authentication auth, Long sessionId) {
        DreamSessionEntity session = ownedSession(auth, sessionId);
        List<DreamMessageEntity> messages = dreamMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (!messages.isEmpty()) {
            String finalInterpretation = ollamaService.summarizeConversation(messages);
            session.setFinalInterpretation(finalInterpretation);
        }
        session.setStatus(SessionStatus.COMPLETED);
        dreamSessionRepository.save(session);
        return toDto(session, toMessageDtos(messages));
    }

    private DreamSessionEntity ownedSession(Authentication auth, Long sessionId) {
        var user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return dreamSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Сессия не найдена или нет доступа"));
    }

    private DreamSessionDto toDto(DreamSessionEntity session, List<DreamMessageDto> messages) {
        return new DreamSessionDto(
                session.getId(),
                session.getTitle(),
                session.getStatus().name(),
                session.getFinalInterpretation(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                messages
        );
    }

    private List<DreamMessageDto> toMessageDtos(List<DreamMessageEntity> messages) {
        return messages.stream()
                .map(m -> new DreamMessageDto(m.getId(), m.getSender().name(), m.getContent(), m.getCreatedAt()))
                .toList();
    }
}
