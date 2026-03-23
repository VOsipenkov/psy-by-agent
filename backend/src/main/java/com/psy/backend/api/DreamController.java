package com.psy.backend.api;

import com.psy.backend.api.dto.CreateDreamSessionRequest;
import com.psy.backend.api.dto.DreamSessionDto;
import com.psy.backend.api.dto.SendMessageRequest;
import com.psy.backend.service.DreamService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dreams")
public class DreamController {

    private final DreamService dreamService;

    public DreamController(DreamService dreamService) {
        this.dreamService = dreamService;
    }

    @GetMapping
    public List<DreamSessionDto> list(Authentication authentication) {
        return dreamService.listSessions(authentication);
    }

    @PostMapping
    public DreamSessionDto create(Authentication authentication, @Valid @RequestBody CreateDreamSessionRequest request) {
        return dreamService.createSession(authentication, request.title());
    }

    @GetMapping("/{id}")
    public DreamSessionDto get(Authentication authentication, @PathVariable Long id) {
        return dreamService.getSession(authentication, id);
    }

    @PostMapping("/{id}/messages")
    public DreamSessionDto sendMessage(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return dreamService.sendMessage(authentication, id, request.content());
    }

    @PostMapping("/{id}/complete")
    public DreamSessionDto complete(Authentication authentication, @PathVariable Long id) {
        return dreamService.completeSession(authentication, id);
    }
}
