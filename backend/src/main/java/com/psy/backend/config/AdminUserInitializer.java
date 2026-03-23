package com.psy.backend.config;

import com.psy.backend.domain.UserEntity;
import com.psy.backend.domain.UserRole;
import com.psy.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminUserInitializer {

    @Bean
    CommandLineRunner createDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                UserEntity admin = new UserEntity();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin"));
                admin.setRole(UserRole.ADMIN);
                userRepository.save(admin);
            }
        };
    }
}
