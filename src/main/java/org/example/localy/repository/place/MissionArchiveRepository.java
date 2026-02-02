package org.example.localy.repository.place;

import org.example.localy.entity.Users;
import org.example.localy.entity.place.MissionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MissionArchiveRepository extends JpaRepository<MissionArchive, Long> {
    void deleteAllByUser(Users user);
    List<MissionArchive> findByUserAndArchivedDateBetween(Users user, LocalDate start, LocalDate end);
}