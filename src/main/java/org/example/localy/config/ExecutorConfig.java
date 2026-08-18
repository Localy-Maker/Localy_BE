package org.example.localy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    // 장소 좌표 보강, GPT 미션 생성 등 서로 독립적인 외부 API 호출을 병렬로 처리하기 위한 스레드 풀
    @Bean(destroyMethod = "shutdown")
    public ExecutorService externalApiExecutor() {
        return Executors.newFixedThreadPool(16);
    }
}
