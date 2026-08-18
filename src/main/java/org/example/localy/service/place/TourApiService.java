package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.place.TourApiDetailDto;
import org.example.localy.dto.place.TourApiDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
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

    public TourApiDetailDto getPlaceDetailByCid(String cid) {
        try {
            log.info("장소 상세 정보 조회 시작. cid: {}", cid);

            TourApiDetailDto response = webClient.post()
                    .uri("https://api-call.visitseoul.net/api/v1/contents/info")
                    .header("VISITSEOUL-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("cid", cid))
                    .retrieve()
                    .bodyToMono(TourApiDetailDto.class)
                    // VisitSeoul 상세 API가 간헐적으로 500을 반환하는 경우가 많아, 일시적인 오류로 보고 짧게 재시도
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(400))
                            .filter(e -> e instanceof WebClientResponseException
                                    && ((WebClientResponseException) e).getStatusCode().is5xxServerError()))
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

    // 목록 조회 (1페이지, 기본 50개) — 기존 호출부 호환용
    public List<TourApiDto.Data> getContentsList() {
        TourApiDto response = getContentsPage(1, 50);
        return response.getData() != null ? response.getData() : new ArrayList<>();
    }

    // 목록 조회 (페이지 지정) — 카탈로그 전체 동기화처럼 여러 페이지를 순회할 때 사용.
    // paging(total_count 등)까지 그대로 반환한다.
    public TourApiDto getContentsPage(int pageNo, int pageRow) {
        try {
            log.info("VisitSeoul API 호출 시작. page_no={}, pageRow={}, API Key: {}",
                    pageNo, pageRow, apiKey != null ? "설정됨" : "미설정");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("page_no", pageNo);
            requestBody.put("pageRow", pageRow);

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
                return emptyResponse();
            }

            if (response.getResultCode() != null && response.getResultCode() != 200) {
                log.error("VisitSeoul API 오류. result_code: {}, message: {}",
                        response.getResultCode(), response.getResultMessage());
                return emptyResponse();
            }

            if (response.getData() == null) {
                response.setData(new ArrayList<>());
            }

            log.info("VisitSeoul API로부터 {}개의 장소를 가져왔습니다. (전체: {}개)",
                    response.getData().size(),
                    response.getPaging() != null ? response.getPaging().getTotalCount() : "unknown");

            return response;

        } catch (Exception e) {
            log.error("VisitSeoul API 호출 중 오류 발생: {}", e.getMessage(), e);
            return emptyResponse();
        }
    }

    private TourApiDto emptyResponse() {
        return TourApiDto.builder().data(new ArrayList<>()).build();
    }
}