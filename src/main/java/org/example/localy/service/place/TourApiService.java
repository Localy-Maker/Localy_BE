package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.config.ExternalApiConfig;
import org.example.localy.dto.place.TourApiDto;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiService {

    private final RestTemplate restTemplate;
    private final ExternalApiConfig apiConfig;

    // 서비스 키 직접 삽입 url 생성
    private String createUrl(String operation, String... params) {
        StringBuilder urlBuilder = new StringBuilder(apiConfig.getTourApiBaseUrl())
                .append("/KorService2/").append(operation).append("2")
                .append("?serviceKey=").append(apiConfig.getTourApiServiceKey())
                .append("&MobileOS=ETC")
                .append("&MobileApp=Localy")
                .append("&_type=json");

        for (int i = 0; i < params.length; i += 2) {
            String key = params[i];
            String value = params[i + 1];

            if (value == null || value.isEmpty()) {
                continue;
            }

            if (key.equals("keyword")) {
                value = URLEncoder.encode(value, StandardCharsets.UTF_8);
            }
            urlBuilder.append("&").append(key).append("=").append(value);
        }

        return urlBuilder.toString();
    }

    // 위치기반 정보 조회
    public List<TourApiDto.LocationBasedItem> getLocationBasedList(
            Double latitude, Double longitude, Integer radius, String contentTypeId) {
        try {
            String urlString = createUrl("locationBasedList",
                    "numOfRows", "100",
                    "pageNo", "1",
                    "mapX", String.valueOf(longitude),
                    "mapY", String.valueOf(latitude),
                    "radius", String.valueOf(radius != null ? radius : 20000),
                    "contentTypeId", contentTypeId != null ? contentTypeId : ""
            );

            URI url = URI.create(urlString);

            log.info("========================================");
            log.info("🌐 한국관광공사 API 호출 URL:");
            log.info("{}", urlString);
            log.info("========================================");

            // ApiResponse 사용 (response 래퍼 포함)
            ResponseEntity<TourApiDto.ApiResponse<TourApiDto.LocationBasedItem>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<TourApiDto.ApiResponse<TourApiDto.LocationBasedItem>>() {}
                    );

            log.info("API 응답 상태: {}", response.getStatusCode());

            if (response.getBody() != null && response.getBody().getResponse() != null) {
                TourApiDto.Response<TourApiDto.LocationBasedItem> apiResponse = response.getBody().getResponse();

                log.info("Response Header: {}", apiResponse.getHeader());

                if (apiResponse.getBody() != null) {
                    log.info("Total Count: {}", apiResponse.getBody().getTotalCount());

                    if (apiResponse.getBody().getItems() != null && apiResponse.getBody().getItems().getItem() != null) {
                        List<TourApiDto.LocationBasedItem> items = apiResponse.getBody().getItems().getItem();
                        log.info("조회된 장소 개수: {}", items.size());
                        return items;
                    }
                }
            }

            log.warn("응답 데이터가 비어있음");
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            log.error("위치 기반 관광정보 조회 실패 - HTTP 에러 {} {}: {}",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            throw new CustomException(PlaceErrorCode.TOUR_API_ERROR);
        } catch (Exception e) {
            log.error("위치 기반 관광정보 조회 실패 - 일반 에러 발생", e);
            throw new CustomException(PlaceErrorCode.TOUR_API_ERROR);
        }
    }

    // 키워드 검색 조회
    public List<TourApiDto.LocationBasedItem> searchByKeyword(String keyword, String contentTypeId) {
        try {
            String urlString = createUrl("searchKeyword",
                    "numOfRows", "50",
                    "pageNo", "1",
                    "keyword", keyword,
                    "contentTypeId", contentTypeId != null ? contentTypeId : ""
            );

            URI url = URI.create(urlString);

            ResponseEntity<TourApiDto.ApiResponse<TourApiDto.LocationBasedItem>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<TourApiDto.ApiResponse<TourApiDto.LocationBasedItem>>() {}
                    );

            if (response.getBody() != null &&
                    response.getBody().getResponse() != null &&
                    response.getBody().getResponse().getBody() != null &&
                    response.getBody().getResponse().getBody().getItems() != null) {
                return response.getBody().getResponse().getBody().getItems().getItem();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", keyword, e);
            throw new CustomException(PlaceErrorCode.TOUR_API_ERROR);
        }
    }

    // 공통정보 상세 조회
    public TourApiDto.CommonItem getCommonDetail(String contentId) {
        try {
            String urlString = createUrl("detailCommon",
                    "contentId", contentId,
                    "defaultYN", "Y",
                    "firstImageYN", "Y",
                    "addrinfoYN", "Y",
                    "overviewYN", "Y"
            );

            URI url = URI.create(urlString);

            ResponseEntity<TourApiDto.ApiResponse<TourApiDto.CommonItem>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<TourApiDto.ApiResponse<TourApiDto.CommonItem>>() {}
                    );

            if (response.getBody() != null &&
                    response.getBody().getResponse() != null &&
                    response.getBody().getResponse().getBody() != null &&
                    response.getBody().getResponse().getBody().getItems() != null &&
                    !response.getBody().getResponse().getBody().getItems().getItem().isEmpty()) {
                return response.getBody().getResponse().getBody().getItems().getItem().get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("공통정보 조회 실패: {}", contentId, e);
            throw new CustomException(PlaceErrorCode.TOUR_API_ERROR);
        }
    }

    // 소개 정보 상세 조회
    public TourApiDto.IntroItem getIntroDetail(String contentId, String contentTypeId) {
        try {
            String urlString = createUrl("detailIntro",
                    "contentId", contentId,
                    "contentTypeId", contentTypeId
            );

            URI url = URI.create(urlString);

            ResponseEntity<TourApiDto.ApiResponse<TourApiDto.IntroItem>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<TourApiDto.ApiResponse<TourApiDto.IntroItem>>() {}
                    );

            if (response.getBody() != null &&
                    response.getBody().getResponse() != null &&
                    response.getBody().getResponse().getBody() != null &&
                    response.getBody().getResponse().getBody().getItems() != null &&
                    !response.getBody().getResponse().getBody().getItems().getItem().isEmpty()) {
                return response.getBody().getResponse().getBody().getItems().getItem().get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("소개정보 조회 실패: {}", contentId, e);
            throw new CustomException(PlaceErrorCode.TOUR_API_ERROR);
        }
    }

    // 이미지 정보 조회
    public List<TourApiDto.ImageItem> getImages(String contentId) {
        try {
            String urlString = createUrl("detailImage",
                    "contentId", contentId,
                    "imageYN", "Y",
                    "numOfRows", "10"
            );

            URI url = URI.create(urlString);

            ResponseEntity<TourApiDto.ApiResponse<TourApiDto.ImageItem>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<TourApiDto.ApiResponse<TourApiDto.ImageItem>>() {}
                    );

            if (response.getBody() != null &&
                    response.getBody().getResponse() != null &&
                    response.getBody().getResponse().getBody() != null &&
                    response.getBody().getResponse().getBody().getItems() != null) {
                return response.getBody().getResponse().getBody().getItems().getItem();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("이미지 정보 조회 실패: {}", contentId, e);
            return Collections.emptyList();
        }
    }
}