package org.example.localy.repository;

import org.example.localy.entity.ChatMessage;
import org.example.localy.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}
