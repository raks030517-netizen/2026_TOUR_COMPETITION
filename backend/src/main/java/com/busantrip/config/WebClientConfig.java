package com.busantrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // API별 인증 방식은 실제 연동 단계에서 각 클라이언트에 설정합니다.
        // 공공데이터포털류 API는 결과 0건일 때 객체 필드를 빈 문자열("")로 내려주는 경우가 있어
        // (예: 연관 관광지 검색 결과 없음 -> "items": "") 빈 문자열 -> null로 관대하게 처리한다.
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .withCoercionConfig(LogicalType.POJO,
                        config -> config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();

        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper)));
    }
}
