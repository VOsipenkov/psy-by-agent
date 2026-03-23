package com.psy.backend.service;

import com.psy.backend.domain.DreamMessageEntity;
import com.psy.backend.domain.DreamSessionEntity;
import com.psy.backend.domain.MessageSender;
import com.psy.backend.domain.SessionStatus;
import com.psy.backend.domain.UserEntity;
import com.psy.backend.repository.DreamMessageRepository;
import com.psy.backend.repository.DreamSessionRepository;
import com.psy.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    private DreamSessionRepository dreamSessionRepository;
    @Mock
    private DreamMessageRepository dreamMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OllamaService ollamaService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private DreamService dreamService;

    @Test
    void createSessionShouldCreateActiveSession() {
        UserEntity user = user(1L, "alice");
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(dreamSessionRepository.save(any(DreamSessionEntity.class))).thenAnswer(invocation -> {
            DreamSessionEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        var result = dreamService.createSession(authentication, "Мой сон");

        assertEquals("ACTIVE", result.status());
        assertEquals("Мой сон", result.title());
    }

    @Test
    void sendMessageShouldPersistUserAndAssistantMessages() {
        UserEntity user = user(1L, "alice");
        DreamSessionEntity session = session(10L, user, SessionStatus.ACTIVE);
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(dreamSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(session));
        when(dreamMessageRepository.findBySessionIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(newMessage(session, MessageSender.USER, "test")))
                .thenReturn(List.of(
                        newMessage(session, MessageSender.USER, "test"),
                        newMessage(session, MessageSender.ASSISTANT, "reply")
                ));
        when(ollamaService.askAssistant(any())).thenReturn("reply");

        var result = dreamService.sendMessage(authentication, 10L, "test");

        verify(dreamMessageRepository, times(2)).save(any(DreamMessageEntity.class));
        assertEquals(2, result.messages().size());
        assertEquals("ASSISTANT", result.messages().get(1).sender());
    }

    @Test
    void completeSessionShouldSetCompletedAndFinalInterpretation() {
        UserEntity user = user(1L, "alice");
        DreamSessionEntity session = session(10L, user, SessionStatus.ACTIVE);
        List<DreamMessageEntity> history = List.of(newMessage(session, MessageSender.USER, "dream"));
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(dreamSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(session));
        when(dreamMessageRepository.findBySessionIdOrderByCreatedAtAsc(10L)).thenReturn(history);
        when(ollamaService.summarizeConversation(history)).thenReturn("summary");

        var result = dreamService.completeSession(authentication, 10L);

        ArgumentCaptor<DreamSessionEntity> captor = ArgumentCaptor.forClass(DreamSessionEntity.class);
        verify(dreamSessionRepository).save(captor.capture());
        assertEquals(SessionStatus.COMPLETED, captor.getValue().getStatus());
        assertEquals("summary", captor.getValue().getFinalInterpretation());
        assertEquals("COMPLETED", result.status());
    }

    @Test
    void getSessionShouldThrowWhenSessionNotOwned() {
        UserEntity user = user(1L, "alice");
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(dreamSessionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> dreamService.getSession(authentication, 10L));
    }

    private static UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static DreamSessionEntity session(Long id, UserEntity user, SessionStatus status) {
        DreamSessionEntity session = new DreamSessionEntity();
        session.setId(id);
        session.setUser(user);
        session.setTitle("title");
        session.setStatus(status);
        return session;
    }

    private static DreamMessageEntity newMessage(DreamSessionEntity session, MessageSender sender, String content) {
        DreamMessageEntity msg = new DreamMessageEntity();
        msg.setSession(session);
        msg.setSender(sender);
        msg.setContent(content);
        return msg;
    }
}
