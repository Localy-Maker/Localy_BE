package org.example.localy.repository.place;

import org.example.localy.entity.place.Bookmark;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserAndPlace(Users user, Place place);

    boolean existsByUserAndPlace(Users user, Place place);

    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.id < :lastBookmarkId ORDER BY b.createdAt DESC")
    List<Bookmark> findByUserAndIdLessThanOrderByCreatedAtDesc(
            @Param("user") Users user,
            @Param("lastBookmarkId") Long lastBookmarkId,
            Pageable pageable);

    @Query("SELECT b FROM Bookmark b WHERE b.user = :user ORDER BY b.createdAt DESC")
    List<Bookmark> findByUserOrderByCreatedAtDesc(@Param("user") Users user, Pageable pageable);

    @Query("SELECT b FROM Bookmark b JOIN b.place p WHERE b.user = :user AND b.id < :lastBookmarkId ORDER BY p.bookmarkCount DESC, b.createdAt DESC")
    List<Bookmark> findByUserAndIdLessThanOrderByPopularity(
            @Param("user") Users user,
            @Param("lastBookmarkId") Long lastBookmarkId,
            Pageable pageable);

    @Query("SELECT b FROM Bookmark b JOIN b.place p WHERE b.user = :user ORDER BY p.bookmarkCount DESC, b.createdAt DESC")
    List<Bookmark> findByUserOrderByPopularity(@Param("user") Users user, Pageable pageable);

    @Query("SELECT b FROM Bookmark b WHERE b.user = :user ORDER BY b.createdAt DESC")
    List<Bookmark> findTop5ByUserOrderByCreatedAtDesc(@Param("user") Users user, Pageable pageable);

    long countByUser(Users user);

    @Modifying
    @Transactional
    void deleteAllByUser(Users user);
}