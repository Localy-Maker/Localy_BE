// src/main/java/org/example/localy/dto/mission/MissionArchiveDto.java

package org.example.localy.dto.mission;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

public class MissionArchiveDto {

    @Getter
    @Builder
    public static class MonthlySummaryResponse {
        private Long userId;
        private int year;
        private int month;
        private List<ArchiveSummary> archives;
    }

    @Getter
    @Builder
    public static class ArchiveSummary {
        private LocalDate date;
        private String thumbnailImageUrl;
        private boolean hasPhoto;
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long userId;
        private LocalDate date;
        private long completedMissionCount; // 해당 일자 완료 미션 수
        private List<PhotoItem> photos;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoItem {
        private Long archiveId;
        private String imageUrl;
        private boolean isThumbnail;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        private String imageUrl;
        private LocalDate targetDate;
        private LocalDate photoStoredDate;
    }
}