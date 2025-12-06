package org.example.localy.repository;

import org.example.localy.entity.ChatMessage;
import org.example.localy.entity.EmotionWindowResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmotionWindowResultRepository extends JpaRepository<EmotionWindowResult, Long> {

    // 오늘 날짜 기준 createdAt 조회
    @Query("SELECT r FROM EmotionWindowResult r WHERE r.createdAt BETWEEN :start AND :end ORDER BY r.createdAt DESC")
    List<EmotionWindowResult> findTodayResults(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<EmotionWindowResult> findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT e.userId FROM EmotionWindowResult e WHERE e.createdAt BETWEEN :start AND :end")
    List<Long> findDistinctUserIdsToday(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 특정 날짜 범위의 결과 조회 (시간순 정렬)
    List<EmotionWindowResult> findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    // 1) 특정 userId에 대해 section별 개수를 세서 가장 많은 section 조회
    @Query("SELECT e.section AS section, COUNT(e) AS cnt " +
            "FROM EmotionWindowResult e " +
            "WHERE e.userId = :userId " +
            "GROUP BY e.section " +
            "ORDER BY cnt DESC")
    List<Object[]> findSectionCountByUser(@Param("userId") Long userId);

    // 2) 특정 userId와 section 값으로 emotion별 개수 조회
    @Query("SELECT e.emotion AS emotion, COUNT(e) AS cnt " +
            "FROM EmotionWindowResult e " +
            "WHERE e.userId = :userId AND e.section = :section " +
            "GROUP BY e.emotion " +
            "ORDER BY cnt DESC")
    List<Object[]> findEmotionCountByUserAndSection(@Param("userId") Long userId,
                                                    @Param("section") Integer section);


    // ⭐ 추가: 특정 기간의 모든 윈도우 결과 조회 (일일 집계용)
    List<EmotionWindowResult> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e.emotion FROM EmotionWindowResult e " +
            "WHERE e.userId = :userId AND e.createdAt BETWEEN :start AND :end")
    List<String> findEmotionsByUserAndWeek(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmotionWindowResult e WHERE e.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
