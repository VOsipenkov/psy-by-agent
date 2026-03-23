package com.psy.backend.repository;

import com.psy.backend.domain.DreamMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DreamMessageRepository extends JpaRepository<DreamMessageEntity, Long> {
    List<DreamMessageEntity> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
