package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.response.PlaceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class NaverLocalClientTest {

    // 2026-07-15 실제 네이버 지역검색 API 호출로 확인한 응답을 그대로 고정값으로 사용한다.
    private static final String REAL_SAMPLE_RESPONSE = """
            {
              "items": [
                {
                  "title": "바운스 유니버스 <b>부산</b>",
                  "address": "부산광역시 기장군 기장읍 시랑리 산46",
                  "roadAddress": "부산광역시 기장군 기장읍 동부산관광로 38",
                  "mapx": "1292159178",
                  "mapy": "351945604"
                }
              ]
            }
            """;

    @Test
    void 검색결과의_HTML태그를_제거하고_좌표를_위경도로_변환한다() {
        NaverLocalClient client = buildClientReturning(HttpStatus.OK, REAL_SAMPLE_RESPONSE);

        StepVerifier.create(client.search("부산 카페"))
                .assertNext(places -> {
                    PlaceResponse place = places.get(0);
                    assertThat(place.name()).isEqualTo("바운스 유니버스 부산");
                    assertThat(place.address()).isEqualTo("부산광역시 기장군 기장읍 동부산관광로 38");
                    assertThat(place.latitude()).isCloseTo(35.1945604, within(1e-6));
                    assertThat(place.longitude()).isCloseTo(129.2159178, within(1e-6));
                })
                .verifyComplete();
    }

    @Test
    void 인증키가_없으면_API를_호출하지_않고_바로_에러를_반환한다() {
        NaverLocalClient client = new NaverLocalClient(WebClient.builder(), new ApiKeyProperties());

        StepVerifier.create(client.search("아무거나"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void 검색어가_비어있으면_바로_에러를_반환한다() {
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.getNaver().getSearch().setClientId("test-id");
        properties.getNaver().getSearch().setClientSecret("test-secret");

        NaverLocalClient client = new NaverLocalClient(WebClient.builder(), properties);

        StepVerifier.create(client.search(" "))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    private NaverLocalClient buildClientReturning(HttpStatus status, String responseBody) {
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse clientResponse = ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(clientResponse));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);

        ApiKeyProperties properties = new ApiKeyProperties();
        properties.getNaver().getSearch().setClientId("test-id");
        properties.getNaver().getSearch().setClientSecret("test-secret");

        return new NaverLocalClient(builder, properties);
    }
}
