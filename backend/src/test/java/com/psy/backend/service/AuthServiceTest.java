package com.psy.backend.service;

import com.psy.backend.api.dto.LoginRequest;
import com.psy.backend.api.dto.RegisterRequest;
import com.psy.backend.domain.UserEntity;
import com.psy.backend.repository.UserRepository;
import com.psy.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("alice", "secret");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        when(jwtService.generateToken("alice")).thenReturn("token");

        var response = authService.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("alice", saved.getUsername());
        assertEquals("hash", saved.getPasswordHash());
        assertEquals("alice", response.username());
        assertEquals("token", response.token());
    }

    @Test
    void registerShouldFailForDuplicateUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(new RegisterRequest("alice", "secret")));
    }

    @Test
    void loginShouldAuthenticateAndReturnToken() {
        when(jwtService.generateToken("alice")).thenReturn("token");
        var response = authService.login(new LoginRequest("alice", "secret"));

        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateToken(eq("alice"));
        assertEquals("alice", response.username());
        assertEquals("token", response.token());
    }
}
