package com.busantrip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "external-api.gemma.api-key=",
                "external-api.weather.service-key=",
                "external-api.avi.service-key="
        }
)
class ItineraryControllerTest {

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
    void planCreatesACompleteDemoItineraryWithoutAnyExternalKeys() {
        webTestClient.post()
                .uri("/api/itineraries/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.source").isEqualTo("guided-demo")
                .jsonPath("$.days.length()").isEqualTo(2)
                .jsonPath("$.orderedPlaces.length()").isEqualTo(6)
                .jsonPath("$.route.fallback").isEqualTo(true)
                .jsonPath("$.context.weather.live").isEqualTo(false)
                .jsonPath("$.days[0].stops[0].time").isEqualTo("10:00");
    }

    @Test
    void adjustmentRebuildsTheItineraryWithTheNewCondition() {
        webTestClient.post()
                .uri("/api/itineraries/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        { "request": %s, "adjustment": "비가 오면 실내 코스로 바꿔줘" }
                        """.formatted(requestBody()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("변경된 바다 · 야경 부산 여행")
                .jsonPath("$.orderedPlaces[0].indoor").isEqualTo(true)
                .jsonPath("$.route.path").isArray();
    }

    private String requestBody() {
        return """
                {
                  "startDate": "2026-07-20",
                  "endDate": "2026-07-21",
                  "themes": ["바다", "야경"],
                  "companion": "친구와",
                  "transport": "대중교통",
                  "pace": "여유롭게",
                  "start": { "latitude": 35.1795543, "longitude": 129.0756416 }
                }
                """;
    }
}
