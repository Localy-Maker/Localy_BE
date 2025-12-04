package org.example.localy.repository;

import org.example.localy.entity.NotificationRead;
import org.example.localy.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Integer> {
    Long countByUserIdAndIsReadFalse(Long userId);

    List<NotificationRead> findAllByUserOrderByNotificationCreatedAtDesc(Users user);
}
