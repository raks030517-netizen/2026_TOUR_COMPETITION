package com.busantrip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "external-api.naver.search.client-id=test-naver-id",
                "external-api.naver.search.client-secret=test-naver-secret",
                "external-api.gemini.api-key=test-gemini-key",
                "external-api.weather.service-key=test-weather-key"
        }
)
class BusanTripApplicationTests {

    @Value("${local.server.port}")
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void healthReturnsUp() {
        webTestClient.get()
                .uri("/api/system/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void configStatusReturnsOnlyConfiguredFlags() {
        webTestClient.get()
                .uri("/api/system/config-status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {
                          "naverSearchConfigured": true,
                          "geminiConfigured": true,
                          "weatherConfigured": true
                        }
                        """)
                .jsonPath("$.naverSearchConfigured").isBoolean()
                .jsonPath("$.geminiConfigured").isBoolean()
                .jsonPath("$.weatherConfigured").isBoolean()
                .jsonPath("$.clientId").doesNotExist()
                .jsonPath("$.clientSecret").doesNotExist()
                .jsonPath("$.apiKey").doesNotExist()
                .jsonPath("$.serviceKey").doesNotExist();
    }
}
