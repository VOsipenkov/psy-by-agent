package com.psy.backend.api;

import com.psy.backend.api.dto.AuthResponse;
import com.psy.backend.api.dto.DreamSessionDto;
import com.psy.backend.security.JwtAuthFilter;
import com.psy.backend.service.AuthService;
import com.psy.backend.service.DreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, DreamController.class, ApiExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ApiControllersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private DreamService dreamService;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void registerShouldReturnCreated() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("token", "alice"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void loginShouldReturnOk() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("token", "alice"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void registerShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Невалидный запрос"));
    }

    @Test
    void listDreamsShouldReturnSessions() throws Exception {
        DreamSessionDto dto = sampleSession(1L, "Первый сон", "ACTIVE");
        when(dreamService.listSessions(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/dreams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Первый сон"));
    }

    @Test
    void createDreamShouldReturnCreatedSession() throws Exception {
        DreamSessionDto dto = sampleSession(2L, "Новый сон", "ACTIVE");
        when(dreamService.createSession(any(), eq("Новый сон"))).thenReturn(dto);

        mockMvc.perform(post("/api/dreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Новый сон"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Новый сон"));
    }

    @Test
    void sendMessageShouldReturnUpdatedSession() throws Exception {
        DreamSessionDto dto = sampleSession(3L, "Сон", "ACTIVE");
        when(dreamService.sendMessage(any(), anyLong(), eq("test"))).thenReturn(dto);

        mockMvc.perform(post("/api/dreams/3/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void completeShouldReturnCompletedSession() throws Exception {
        DreamSessionDto dto = sampleSession(4L, "Завершенный сон", "COMPLETED");
        when(dreamService.completeSession(any(), eq(4L))).thenReturn(dto);

        mockMvc.perform(post("/api/dreams/4/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private DreamSessionDto sampleSession(Long id, String title, String status) {
        return new DreamSessionDto(
                id,
                title,
                status,
                null,
                Instant.now(),
                Instant.now(),
                List.of()
        );
    }
}
