package org.example.localy.repository;

import org.example.localy.entity.NotificationRead;
import org.example.localy.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Integer> {
    Long countByUserIdAndIsReadFalse(Long userId);

    List<NotificationRead> findAllByUserOrderByNotificationCreatedAtDesc(Users user);

    @Modifying
    @Transactional
    void deleteAllByUser(Users user);
}
