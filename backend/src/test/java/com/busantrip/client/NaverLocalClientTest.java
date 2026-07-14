package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.place.NaverLocalApiResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class NaverLocalClientTest {

    @Test
    void mapsNaverJsonResponseAndSendsRequiredRequestValues() {
        String body = """
                {
                  "total": 1,
                  "items": [
                    {
                      "title": "<b>해운대</b>해수욕장",
                      "link": "https://example.com/place",
                      "category": "여행,명소>해수욕장",
                      "description": "ignored",
                      "telephone": "",
                      "address": "부산광역시 해운대구 우동",
                      "roadAddress": "부산광역시 해운대구 해운대해변로 264",
                      "mapx": "1291603840",
                      "mapy": "351593251"
                    }
                  ]
                }
                """;
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                    .body(body)
                    .build());
        };
        ApiKeyProperties properties = apiKeyProperties();
        NaverLocalClient client = new NaverLocalClient(
                WebClient.builder().exchangeFunction(exchangeFunction), properties);

        NaverLocalApiResponse response = client.search("부산 해운대 관광지").block();

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst())
                .extracting(
                        NaverLocalApiResponse.Item::title,
                        NaverLocalApiResponse.Item::mapx,
                        NaverLocalApiResponse.Item::mapy)
                .containsExactly("<b>해운대</b>해수욕장", 1_291_603_840L, 351_593_251L);

        ClientRequest request = capturedRequest.get();
        assertThat(request.url().getPath()).isEqualTo("/v1/search/local.json");
        assertThat(request.url().getQuery()).contains(
                "query=부산 해운대 관광지",
                "display=5",
                "start=1",
                "sort=random");
        assertThat(request.headers().getFirst("X-Naver-Client-Id")).isEqualTo("test-client-id");
        assertThat(request.headers().getFirst("X-Naver-Client-Secret")).isEqualTo("test-client-secret");
    }

    private ApiKeyProperties apiKeyProperties() {
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.getNaver().getSearch().setClientId("test-client-id");
        properties.getNaver().getSearch().setClientSecret("test-client-secret");
        return properties;
    }
}
