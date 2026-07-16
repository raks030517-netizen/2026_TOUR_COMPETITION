package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.busantrip.config.OdsayProperties;
import com.busantrip.dto.external.route.OdsayWalkResponse;
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

class OdsayWalkClientTest {

    @Test
    void sendsWalkV2RequestAndDecodesActualResponseFields() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(successBody())
                    .build());
        };
        OdsayWalkClient client = client(exchange, "test+/=");

        OdsayWalkResponse response = client.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block();

        assertThat(response).isNotNull();
        assertThat(response.result().path()).hasSize(1);
        assertThat(response.result().path().getFirst().recommend().summary().distance())
                .isEqualTo(1_540);
        assertThat(response.result().path().getFirst().recommend().routes().getFirst().coordinate())
                .hasSize(2);
        ClientRequest request = captured.get();
        assertThat(request.url().getPath()).isEqualTo("/v1/api/searchWalkPathV2");
        assertThat(request.url().getQuery()).contains(
                "opt=reco", "loc=129.0756,35.1795,129.086,35.169",
                "lang=0", "output=json", "apiKey=");
        assertThat(request.url().getRawQuery()).contains("apiKey=test%2B%2F%3D");
        assertThat(request.url().toASCIIString()).doesNotContain("test+/=");
    }

    @Test
    void mapsUnavailableProductAndMissingConfigurationToClearCodes() {
        OdsayWalkClient forbidden = client(
                request -> Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build()),
                "server-key");
        OdsayWalkClient missing = client(
                request -> Mono.error(new AssertionError("외부 요청이 실행되면 안 됩니다.")),
                "");

        Throwable unavailable = catchThrowable(() -> forbidden.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block());
        Throwable configuration = catchThrowable(() -> missing.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block());

        assertThat(((RouteApiException) unavailable).getCode()).isEqualTo("WALK_API_UNAVAILABLE");
        assertThat(((RouteApiException) configuration).getCode()).isEqualTo("WALK_CONFIGURATION_ERROR");
    }

    private OdsayWalkClient client(ExchangeFunction exchangeFunction, String key) {
        OdsayProperties properties = new OdsayProperties();
        properties.setKey(key);
        properties.setBaseUrl("https://example.test/v1/api");
        return new OdsayWalkClient(
                WebClient.builder().exchangeFunction(exchangeFunction), properties);
    }

    private String successBody() {
        return """
                {
                  "result": {
                    "path": [{
                      "hasPathResult": true,
                      "speedKMPerHour": 4.2,
                      "recommend": {
                        "summary": {
                          "start": {"x": 129.0756, "y": 35.1795},
                          "end": {"x": 129.0860, "y": 35.1690},
                          "distance": 1540,
                          "duration": 1320
                        },
                        "routes": [{
                          "type": 11,
                          "facility": 0,
                          "distance": 1540,
                          "duration": 1320,
                          "coordinate": [
                            {"x": 129.0756, "y": 35.1795},
                            {"x": 129.0860, "y": 35.1690}
                          ]
                        }],
                        "guides": [{
                          "routeIndex": 0,
                          "angle": 0,
                          "turn": 0,
                          "facility": 0,
                          "guidance": "직진하세요.",
                          "point": {"x": 129.0756, "y": 35.1795}
                        }]
                      }
                    }]
                  }
                }
                """;
    }
}
