package org.example.localy.repository;

import org.example.localy.entity.EmotionDayResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmotionDayResultRepository extends JpaRepository<EmotionDayResult, Long> {

    Optional<EmotionDayResult> findByUserIdAndDate(Long userId, LocalDate date);

    @Query("SELECT COUNT(e) > 0 FROM EmotionDayResult e WHERE e.userId = :userId AND e.date = :date")
    boolean existsByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<EmotionDayResult> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT e.avgScore FROM EmotionDayResult e " +
            "WHERE e.userId = :userId AND e.date BETWEEN :start AND :end")
    List<Double> findAvgScoresByDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);


}