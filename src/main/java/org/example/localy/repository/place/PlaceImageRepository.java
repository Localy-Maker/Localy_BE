package org.example.localy.repository.place;

import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {

    List<PlaceImage> findByPlaceOrderByDisplayOrder(Place place);
}