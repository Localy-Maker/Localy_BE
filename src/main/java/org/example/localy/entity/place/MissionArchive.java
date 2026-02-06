package org.example.localy.entity.place;

import jakarta.persistence.*;
import lombok.*;
import org.example.localy.entity.Users;
import java.time.LocalDate;

@Entity
@Table(name = "mission_archives")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MissionArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDate archivedDate; // 달력상 날짜

    @Builder.Default
    @Column(nullable = false)
    private Boolean isThumbnail = false; // 먼슬리 썸네일 노출 여부
}