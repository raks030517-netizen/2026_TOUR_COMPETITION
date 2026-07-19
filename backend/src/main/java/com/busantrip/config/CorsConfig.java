package com.busantrip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfig implements WebFluxConfigurer {

    private static final String LOCALHOST_ORIGIN_PATTERN = "http://localhost:*";
    private static final String LOOPBACK_ORIGIN_PATTERN = "http://127.0.0.1:*";

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origins[0]}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // Keep the explicitly configured deployment origin while allowing
                // Vite's dynamically selected local development port.
                .allowedOriginPatterns(
                        allowedOrigin,
                        LOCALHOST_ORIGIN_PATTERN,
                        LOOPBACK_ORIGIN_PATTERN
                )
                .allowedMethods(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.OPTIONS.name()
                );
    }
}
