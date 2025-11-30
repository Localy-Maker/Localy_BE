package org.example.localy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ExternalApiConfig {

    private final ObjectMapper objectMapper;

    @Value("${app.tour-api.base-url}")
    private String tourApiBaseUrl;

    @Value("${app.tour-api.service-key}")
    private String tourApiServiceKey;

    // 💡 FIX: 새로운 페이지네이션 및 디테일 정책 값 추가
    @Value("${app.tour-api.default-num-of-rows:100}")
    private String defaultNumOfRows;

    @Value("${app.tour-api.default-page-no:1}")
    private String defaultPageNo;

    @Value("${app.tour-api.default-image-rows:10}")
    private String defaultImageRows;

    @Value("${app.tour-api.detail-default-yn:Y}")
    private String detailDefaultYn;

    @Value("${app.tour-api.detail-first-image-yn:Y}")
    private String detailFirstImageYn;

    @Value("${app.tour-api.detail-addr-info-yn:Y}")
    private String detailAddrInfoYn;

    @Value("${app.tour-api.detail-overview-yn:Y}")
    private String detailOverviewYn;


    @Bean
    public RestTemplate restTemplate() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(converter));

        return restTemplate;
    }

    public String getTourApiBaseUrl() {
        return tourApiBaseUrl;
    }

    public String getTourApiServiceKey() {
        return tourApiServiceKey;
    }

    // 💡 FIX: Getter 추가
    public String getDefaultNumOfRows() {
        return defaultNumOfRows;
    }

    public String getDefaultPageNo() {
        return defaultPageNo;
    }

    public String getDefaultImageRows() {
        return defaultImageRows;
    }

    public String getDetailDefaultYn() {
        return detailDefaultYn;
    }

    public String getDetailFirstImageYn() {
        return detailFirstImageYn;
    }

    public String getDetailAddrInfoYn() {
        return detailAddrInfoYn;
    }

    public String getDetailOverviewYn() {
        return detailOverviewYn;
    }
}