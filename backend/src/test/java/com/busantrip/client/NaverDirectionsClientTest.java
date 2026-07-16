package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.busantrip.config.NaverMapsProperties;
import com.busantrip.dto.external.route.NaverDirectionsResponse;
import com.busantrip.exception.RouteApiException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class NaverDirectionsClientTest {

    @Test
    void sendsDirections5RequestAndMapsResponse() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(successBody())
                    .build());
        };
        NaverDirectionsClient client = client(exchange);

        NaverDirectionsResponse response = client.getDrivingRoute(
                129.0756, 35.1795, 129.1604, 35.1587, "traoptimal").block();

        assertThat(response).isNotNull();
        assertThat(response.code()).isZero();
        assertThat(response.route().get("traoptimal")).hasSize(1);
        ClientRequest request = captured.get();
        assertThat(request.url().getPath()).isEqualTo("/map-direction/v1/driving");
        assertThat(request.url().getQuery()).contains(
                "start=129.0756,35.1795",
                "goal=129.1604,35.1587",
                "option=traoptimal",
                "lang=ko");
        assertThat(request.headers().getFirst("x-ncp-apigw-api-key-id"))
                .isEqualTo("test-key-id");
        assertThat(request.headers().getFirst("x-ncp-apigw-api-key"))
                .isEqualTo("test-server-key");
    }

    @Test
    void mapsAuthenticationFailureWithoutExposingKey() {
        ExchangeFunction exchange = request -> Mono.just(
                ClientResponse.create(HttpStatus.UNAUTHORIZED).build());
        NaverDirectionsClient client = client(exchange);

        Throwable error = catchThrowable(() -> client.getDrivingRoute(
                129.0756, 35.1795, 129.1604, 35.1587, "traoptimal").block());

        assertThat(error).isInstanceOf(RouteApiException.class);
        assertThat(((RouteApiException) error).getCode())
                .isEqualTo("NAVER_MAPS_AUTHENTICATION_FAILED");
        assertThat(error.getMessage()).doesNotContain("test-server-key");
    }

    private NaverDirectionsClient client(ExchangeFunction exchangeFunction) {
        NaverMapsProperties properties = new NaverMapsProperties();
        properties.setKeyId("test-key-id");
        properties.setKey("test-server-key");
        properties.setBaseUrl("https://example.test/map-direction/v1");
        return new NaverDirectionsClient(
                WebClient.builder().exchangeFunction(exchangeFunction),
                properties);
    }

    private String successBody() {
        return """
                {
                  "code": 0,
                  "message": "길찾기를 성공하였습니다.",
                  "route": {
                    "traoptimal": [{
                      "summary": {
                        "distance": 12345,
                        "duration": 1800500,
                        "tollFare": 1000,
                        "taxiFare": 18000,
                        "fuelPrice": 2100
                      },
                      "path": [[129.0756,35.1795],[129.1604,35.1587]],
                      "guide": []
                    }]
                  }
                }
                """;
    }
}
