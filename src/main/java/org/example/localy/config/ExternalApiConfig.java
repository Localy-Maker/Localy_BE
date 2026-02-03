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

    @Value("${app.tour-api.base-url")
    private String tourApiBaseUrl;

    @Value("${app.tour-api.service-key}")
    private String tourApiServiceKey;

    public String getTourApiBaseUrl() {
        return tourApiBaseUrl;
    }

    public String getTourApiServiceKey() {
        return tourApiServiceKey;
    }


    @Bean
    public RestTemplate restTemplate() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(converter));

        return restTemplate;
    }
}