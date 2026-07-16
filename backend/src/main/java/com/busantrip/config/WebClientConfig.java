package com.busantrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .withCoercionConfig(LogicalType.POJO,
                        config -> config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
        return WebClient.builder().codecs(configurer -> configurer.defaultCodecs()
                .jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper)));
    }
}

