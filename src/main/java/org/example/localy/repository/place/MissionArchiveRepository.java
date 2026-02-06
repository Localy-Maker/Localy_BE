package org.example.localy.repository.place;

import org.example.localy.entity.Users;
import org.example.localy.entity.place.MissionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MissionArchiveRepository extends JpaRepository<MissionArchive, Long> {
    List<MissionArchive> findByUserAndArchivedDate(Users user, LocalDate archivedDate);
    List<MissionArchive> findByUserAndArchivedDateBetween(Users user, LocalDate startDate, LocalDate endDate);
    void deleteAllByUser(Users user);

    @Modifying
    @Query("UPDATE MissionArchive a SET a.isThumbnail = false WHERE a.user = :user AND a.archivedDate = :date")
    void resetThumbnails(@Param("user") Users user, @Param("date") LocalDate date);
}