package com.busantrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // API별 인증 방식은 실제 연동 단계에서 각 클라이언트에 설정합니다.
        return WebClient.builder();
    }
}
