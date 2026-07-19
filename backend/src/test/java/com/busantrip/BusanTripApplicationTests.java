package com.busantrip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "external-api.naver.search.client-id=test-naver-id",
                "external-api.naver.search.client-secret=test-naver-secret",
                "external-api.gemma.api-key=test-gemma-key",
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
                          "gemmaConfigured": true,
                          "weatherConfigured": true
                        }
                        """)
                .jsonPath("$.naverSearchConfigured").isBoolean()
                .jsonPath("$.gemmaConfigured").isBoolean()
                .jsonPath("$.weatherConfigured").isBoolean()
                .jsonPath("$.clientId").doesNotExist()
                .jsonPath("$.clientSecret").doesNotExist()
                .jsonPath("$.apiKey").doesNotExist()
                .jsonPath("$.serviceKey").doesNotExist();
    }

    @Test
    void tourismSearchProvidesMappedDemoPlacesWhenNoTourismKeyIsPresent() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tourism/related/search")
                        .queryParam("baseYm", "202504")
                        .queryParam("signguCd", "26350")
                        .queryParam("keyword", "야경")
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.places").isArray()
                .jsonPath("$.places[0].latitude").isNumber()
                .jsonPath("$.places[0].longitude").isNumber();
    }

    @Test
    void routeOptimizationUsesFallbackWhenDirectionsCredentialsAreMissing() {
        webTestClient.post()
                .uri("/api/routes/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "start": { "latitude": 35.1795543, "longitude": 129.0756416 },
                          "places": [
                            {
                              "id": "gwanganri",
                              "name": "광안리해수욕장",
                              "description": "야경 명소",
                              "address": "부산 수영구",
                              "image": "",
                              "category": "야경",
                              "distance": "",
                              "latitude": 35.1532,
                              "longitude": 129.1186
                            }
                          ],
                          "option": "trafast"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.fallback").isEqualTo(true)
                .jsonPath("$.orderedPlaces[0].name").isEqualTo("광안리해수욕장")
                .jsonPath("$.path").isArray();
    }
}
