package org.example.localy.repository.place;

import org.example.localy.entity.place.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByContentId(String contentId);

    List<Place> findByContentIdIn(List<String> contentIds);

    boolean existsByContentId(String contentId);
}