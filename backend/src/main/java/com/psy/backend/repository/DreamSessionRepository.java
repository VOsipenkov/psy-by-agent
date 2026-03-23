package com.psy.backend.repository;

import com.psy.backend.domain.DreamSessionEntity;
import com.psy.backend.domain.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DreamSessionRepository extends JpaRepository<DreamSessionEntity, Long> {
    List<DreamSessionEntity> findByUserOrderByUpdatedAtDesc(UserEntity user);
    Optional<DreamSessionEntity> findByIdAndUserId(Long id, Long userId);
}
