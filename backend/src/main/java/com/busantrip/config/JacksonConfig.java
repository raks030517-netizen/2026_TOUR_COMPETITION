package com.busantrip.config;

import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    /**
     * 외부 API가 객체 대신 빈 문자열("")을 반환하면 null로 변환한다.
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
     * JsonMapper 설정을 모든 WebClient.Builder에 적용한다.
     */
    @Bean
    public WebClientCustomizer jacksonWebClientCustomizer(
            JsonMapper jsonMapper
    ) {
        return webClientBuilder -> webClientBuilder.codecs(
                codecConfigurer -> {
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
                }
        );
    }
}