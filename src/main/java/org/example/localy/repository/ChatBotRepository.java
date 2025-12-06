package org.example.localy.repository;

import org.example.localy.entity.ChatMessage;
import org.example.localy.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatBotRepository extends JpaRepository<ChatMessage, Long> {
    Optional<ChatMessage> findByUserId(Long userId);

    @Query("SELECT c FROM ChatMessage c " +
            "WHERE c.userId = :userId " +
            "AND DATE(c.createdAt) = CURRENT_DATE")
    List<ChatMessage> findTodayMessagesByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT c.createdAt
    FROM ChatMessage c
    WHERE c.userId = :userId
      AND c.createdAt < CURRENT_DATE
    ORDER BY c.createdAt DESC
""")
    List<LocalDateTime> findLastChatDate(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c " +
            "FROM ChatMessage c " +
            "WHERE c.userId = :userId " +
            "AND function('date', c.createdAt) = :date")
    List<ChatMessage> findMessagesByUserIdAndDate(@Param("userId") Long userId,
                                                  @Param("date") LocalDate date);

    @Query("""
    SELECT DISTINCT c.createdAt
    FROM ChatMessage c
    WHERE c.userId = :userId
      AND c.createdAt < CURRENT_DATE
    ORDER BY c.createdAt DESC
""")
    List<LocalDateTime> findPastChatDates(@Param("userId") Long userId);

    // 특정 유저의 특정 날짜 메시지 삭제
    @Modifying
    @Query("""
    DELETE FROM ChatMessage c
    WHERE c.userId = :userId
      AND DATE(c.createdAt) = :date
""")
    void deleteMessagesByUserIdAndDate(@Param("userId") Long userId,
                                       @Param("date") LocalDate date);

    List<ChatMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<ChatMessage> findByRoleAndCreatedAtBetween(ChatMessage.Role role, LocalDateTime start, LocalDateTime end);

    List<ChatMessage> findByUserIdAndCreatedAtBetween(
            Long userId, LocalDateTime start, LocalDateTime end
    );

    @Query("SELECT DISTINCT m.userId FROM ChatMessage m WHERE m.createdAt BETWEEN :start AND :end")
    List<Long> findDistinctUserIdsToday(LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT m.emotionAfter FROM ChatMessage m 
    WHERE m.userId = :userId
    AND m.createdAt BETWEEN :start AND :end
    AND m.emotionAfter IS NOT NULL
    ORDER BY m.createdAt DESC LIMIT 1""")
    Integer findLatestEmotionAfterToday(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.userId = :userId AND c.role = 'USER' AND c.createdAt >= :startOfDay AND c.createdAt < :endOfDay")
    Long countTodayUserMessages(@Param("userId") Long userId,
                                @Param("startOfDay") LocalDateTime startOfDay,
                                @Param("endOfDay") LocalDateTime endOfDay);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage c WHERE c.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
