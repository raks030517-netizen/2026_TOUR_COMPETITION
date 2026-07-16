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
                "external-api.gemma.api-key=test-gemma-key",
                "external-api.gemma.model=test-gemma-model",
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
    void configStatusRequiresAuthentication() {
        webTestClient.get()
                .uri("/api/system/config-status")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    void placeSearchRejectsMissingQuery() {
        webTestClient.get()
                .uri("/api/places/search")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void placeSearchRejectsBlankQuery() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/places/search")
                        .queryParam("query", "   ")
                        .build())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void llmAnalyzeRejectsBlankMessage() {
        webTestClient.post()
                .uri("/api/llm/analyze")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {"message":"   "}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void llmAnalyzeRejectsOutsideBusanRequestWithoutCallingGemma() {
        webTestClient.post()
                .uri("/api/llm/analyze")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {"message":"서울 강남 관광지를 알려주세요."}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void travelSearchRejectsBlankMessage() {
        webTestClient.post()
                .uri("/api/travel/search")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {"message":"   "}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }
}
