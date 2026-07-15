package com.busantrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * 외부 API JSON 변환 설정.
 *
 * 한국관광공사 API는 검색 결과가 없을 때
 * items 객체 대신 빈 문자열("")을 반환할 수 있다.
 *
 * 예:
 * "items": ""
 */
@Configuration
public class JacksonConfig {

    /**
     * 빈 문자열을 복합 객체의 null 값으로 처리하는
     * Jackson 3용 JsonMapper.
     */
    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .enable(
                        DeserializationFeature
                                .ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
                )
                .build();
    }

    /**
     * 위 JsonMapper를 사용하는 WebClient 변환 전략.
     */
    @Bean
    public ExchangeStrategies exchangeStrategies(
            JsonMapper jsonMapper
    ) {
        return ExchangeStrategies.builder()
                .codecs(codecConfigurer -> {
                    codecConfigurer
                            .defaultCodecs()
                            .jacksonJsonDecoder(
                                    new JacksonJsonDecoder(jsonMapper)
                            );

                    codecConfigurer
                            .defaultCodecs()
                            .jacksonJsonEncoder(
                                    new JacksonJsonEncoder(jsonMapper)
                            );
                })
                .build();
    }
}