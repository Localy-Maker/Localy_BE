package org.example.localy.repository;

import org.example.localy.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Integer> {
    Long countByUserIdAndIsReadFalse(Long userId);
}
