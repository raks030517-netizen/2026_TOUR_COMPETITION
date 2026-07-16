package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.busantrip.config.OdsayProperties;
import com.busantrip.dto.external.route.OdsayTransitResponse;
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

class OdsayTransitClientTest {

    @Test
    void sendsServerRequestWithCoordinatesAndDecodesActualFields() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(successBody())
                    .build());
        };
        OdsayTransitClient client = client(exchange, "test+/=");

        OdsayTransitResponse response = client.findRoutes(
                129.0756, 35.1795, 129.1604, 35.1587).block();

        assertThat(response).isNotNull();
        assertThat(response.result().path()).hasSize(1);
        assertThat(response.result().path().getFirst().info().totalTime()).isEqualTo(43);
        assertThat(response.result().path().getFirst().subPath().get(1).lane().getFirst().busNo())
                .isEqualTo("1003");
        ClientRequest request = captured.get();
        assertThat(request.url().getPath()).isEqualTo("/v1/api/searchPubTransPathT");
        assertThat(request.url().getQuery()).contains(
                "SX=129.0756", "SY=35.1795", "EX=129.1604", "EY=35.1587",
                "OPT=0", "SearchType=0", "SearchPathType=0", "lang=0", "apiKey=");
        assertThat(request.url().getRawQuery()).contains("apiKey=test%2B%2F%3D");
        assertThat(request.url().toASCIIString()).doesNotContain("test+/=");
    }

    @Test
    void rejectsMissingConfigurationWithoutExposingKey() {
        OdsayTransitClient client = client(
                request -> Mono.error(new AssertionError("외부 요청이 실행되면 안 됩니다.")), "");

        Throwable error = catchThrowable(() -> client.findRoutes(
                129.0756, 35.1795, 129.1604, 35.1587).block());

        assertThat(error).isInstanceOf(RouteApiException.class);
        assertThat(((RouteApiException) error).getCode()).isEqualTo("TRANSIT_CONFIGURATION_ERROR");
    }

    @Test
    void decodesProviderAuthenticationErrorShape() {
        ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("""
                        {"error":[{"code":"500","message":"[ApiKeyAuthFailed] ApiKey authentication failed."}]}
                        """)
                .build());

        OdsayTransitResponse response = client(exchange, "server-key").findRoutes(
                129.0756, 35.1795, 129.1604, 35.1587).block();

        assertThat(response.error()).hasSize(1);
        assertThat(response.error().getFirst().code()).isEqualTo(500);
    }

    private OdsayTransitClient client(ExchangeFunction exchangeFunction, String key) {
        OdsayProperties properties = new OdsayProperties();
        properties.setKey(key);
        properties.setBaseUrl("https://example.test/v1/api");
        return new OdsayTransitClient(
                WebClient.builder().exchangeFunction(exchangeFunction), properties);
    }

    private String successBody() {
        return """
                {
                  "result": {
                    "searchType": 0,
                    "path": [{
                      "pathType": 2,
                      "info": {
                        "totalWalk": 710,
                        "totalTime": 43,
                        "payment": 1550,
                        "totalDistance": 11234.5
                      },
                      "subPath": [{
                        "trafficType": 3,
                        "distance": 240,
                        "sectionTime": 4
                      }, {
                        "trafficType": 2,
                        "distance": 10400,
                        "sectionTime": 34,
                        "stationCount": 18,
                        "lane": [{"busNo": "1003", "type": 11}],
                        "startName": "부산역",
                        "endName": "해운대해수욕장입구"
                      }]
                    }]
                  }
                }
                """;
    }
}
