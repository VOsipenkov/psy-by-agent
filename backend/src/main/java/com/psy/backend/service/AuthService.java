package com.psy.backend.service;

import com.psy.backend.api.dto.AuthResponse;
import com.psy.backend.api.dto.LoginRequest;
import com.psy.backend.api.dto.RegisterRequest;
import com.psy.backend.domain.UserEntity;
import com.psy.backend.domain.UserRole;
import com.psy.backend.repository.UserRepository;
import com.psy.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Пользователь уже существует");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user.getUsername()), user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        return new AuthResponse(jwtService.generateToken(request.username()), request.username());
    }
}
