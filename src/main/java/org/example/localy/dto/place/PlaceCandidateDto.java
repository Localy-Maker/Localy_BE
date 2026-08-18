package org.example.localy.dto.place;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 키워드로 VisitSeoul 콘텐츠를 검색해 실제 좌표까지 확인할 때 사용하는 후보 DTO
@Getter
@Builder
@AllArgsConstructor
public class PlaceCandidateDto {
    private String cid;
    private String title;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}
