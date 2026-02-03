package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.place.TourApiDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {
    private final WebClient webClient;

    @Value("${app.tour-api.service-key}")
    private String apiKey;

    // VisitSeoul 상세 정보 조회 (POST 방식)
    public TourApiDto getPlaceDetailByCid(String cid) {
        try {
            log.info("장소 상세 정보 조회 시작. cid: {}", cid);

            TourApiDto response = webClient.post()
                    .uri("https://api-call.visitseoul.net/api/v1/contents/info")
                    .header("VISITSEOUL-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("cid", cid))
                    .retrieve()
                    .bodyToMono(TourApiDto.class)
                    .block();

            if (response == null) {
                log.error("장소 상세 정보 API 응답이 null입니다. cid: {}", cid);
                return null;
            }

            if (response.getResultCode() != null && response.getResultCode() != 200) {
                log.error("장소 상세 정보 조회 실패. cid: {}, result_code: {}, message: {}",
                        cid, response.getResultCode(), response.getResultMessage());
                return null;
            }

            if (response.getData() == null) {
                log.error("장소 상세 정보의 data가 null입니다. cid: {}", cid);
                return null;
            }

            log.info("장소 상세 정보 조회 성공. cid: {}, 장소명: {}", cid, response.getData().getPost_sj());
            return response;

        } catch (Exception e) {
            log.error("장소 상세 정보 API 호출 오류. cid: {}, error: {}", cid, e.getMessage(), e);
            return null;
        }
    }

    public List<TourApiDto.Data> getContentsList() {
        try {
            log.info("VisitSeoul API 호출 시작. API Key: {}", apiKey != null ? "설정됨" : "미설정");

            // Swagger 문서에 따른 올바른 요청 파라미터
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("page_no", 1);      // 페이지 번호
            requestBody.put("pageRow", 50);     // 페이지당 개수
            // sort_type: latest (기본값이므로 생략 가능)

            // 원본 응답을 String으로 먼저 받아서 로깅
            String rawResponse = webClient.post()
                    .uri("https://api-call.visitseoul.net/api/v1/contents/list")
                    .header("VISITSEOUL-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("VisitSeoul API 원본 응답: {}", rawResponse);

            // 다시 실제 호출
            TourApiDto response = webClient.post()
                    .uri("https://api-call.visitseoul.net/api/v1/contents/list")
                    .header("VISITSEOUL-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(TourApiDto.class)
                    .block();

            if (response == null) {
                log.error("VisitSeoul API 응답이 null입니다.");
                return new ArrayList<>();
            }

            if (response.getResultCode() != null && response.getResultCode() != 200) {
                log.error("VisitSeoul API 오류. result_code: {}, message: {}",
                        response.getResultCode(), response.getResultMessage());
                return new ArrayList<>();
            }

            log.info("파싱된 TourApiDto - result_code: {}, list: {}, paging: {}",
                    response.getResultCode(),
                    response.getList() != null ? response.getList().size() : "null",
                    response.getPaging());

            if (response.getList() == null || response.getList().isEmpty()) {
                log.error("VisitSeoul API 응답의 list가 null이거나 비어있습니다.");
                return new ArrayList<>();
            }

            log.info("VisitSeoul API로부터 {}개의 장소를 가져왔습니다. (전체: {}개)",
                    response.getList().size(),
                    response.getPaging() != null ? response.getPaging().getTotalCount() : "unknown");

            return response.getList();

        } catch (Exception e) {
            log.error("VisitSeoul API 호출 중 오류 발생: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}