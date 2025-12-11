package org.example.localy.entity.place;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contentId;  // 관광공사 API contentId

    @Column(nullable = false)
    private String contentTypeId;  // 12:관광지, 14:문화시설, 39:음식점 등

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String category;  // cat3 기반 카테고리

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String addressDetail;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 50)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String openingHours;

    @Column(columnDefinition = "TEXT")
    private String thumbnailImage;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String longDescription;

    @Column(nullable = false)
    private Integer bookmarkCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void incrementBookmarkCount() {
        this.bookmarkCount++;
    }

    public void decrementBookmarkCount() {
        if (this.bookmarkCount > 0) {
            this.bookmarkCount--;
        }
    }
}