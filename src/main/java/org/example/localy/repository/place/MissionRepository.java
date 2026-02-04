package org.example.localy.repository.place;

import org.example.localy.entity.place.Mission;
import org.example.localy.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query("SELECT m FROM Mission m WHERE m.user = :user AND m.expiresAt > :now ORDER BY m.createdAt DESC")
    List<Mission> findActiveByUser(@Param("user") Users user, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(m) FROM Mission m WHERE m.user = :user AND m.expiresAt > :now")
    long countActiveByUser(@Param("user") Users user, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(m) FROM Mission m WHERE m.user = :user AND m.isCompleted = true AND m.expiresAt > :now")
    long countCompletedByUser(@Param("user") Users user, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    void deleteAllByUser(Users user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Mission m WHERE m.expiresAt < :now")
    int deleteExpiredMissions(@Param("now") LocalDateTime now);

    @Query("SELECT m FROM Mission m WHERE m.isCompleted = false AND m.expiresAt > :now AND m.expiresAt <= :threeHoursLater")
    List<Mission> findCompletedMissionsExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("threeHoursLater") LocalDateTime threeHoursLater
    );

    @Query("SELECT m.id FROM Mission m WHERE m.expiresAt < :now")
    List<Long> findExpiredMissionIds(@Param("now") LocalDateTime now);
}