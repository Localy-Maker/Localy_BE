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

            if (response.getData() == null || response.getData().isEmpty()) {
                log.error("장소 상세 정보의 data가 null이거나 비어있습니다. cid: {}", cid);
                return null;
            }

            log.info("장소 상세 정보 조회 성공. cid: {}, 장소명: {}", cid, response.getData().get(0).getPost_sj());
            return response;

        } catch (Exception e) {
            log.error("장소 상세 정보 API 호출 오류. cid: {}, error: {}", cid, e.getMessage(), e);
            return null;
        }
    }

    // 목록 조회
    public List<TourApiDto.Data> getContentsList() {
        try {
            log.info("VisitSeoul API 호출 시작. API Key: {}", apiKey != null ? "설정됨" : "미설정");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("page_no", 1);
            requestBody.put("pageRow", 50);

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

            if (response.getData() == null || response.getData().isEmpty()) {
                log.error("VisitSeoul API 응답의 data가 null이거나 비어있습니다.");
                return new ArrayList<>();
            }

            log.info("VisitSeoul API로부터 {}개의 장소를 가져왔습니다. (전체: {}개)",
                    response.getData().size(),
                    response.getPaging() != null ? response.getPaging().getTotalCount() : "unknown");

            return response.getData();

        } catch (Exception e) {
            log.error("VisitSeoul API 호출 중 오류 발생: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}