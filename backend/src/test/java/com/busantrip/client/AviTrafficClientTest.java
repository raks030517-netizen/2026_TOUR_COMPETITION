package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.traffic.AviTrafficApiResponse;
import com.busantrip.exception.AviTrafficApiException;
import com.busantrip.exception.AviTrafficAuthenticationException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

class AviTrafficClientTest {

    @Test
    void mapsOfficialJsonFieldsAndSendsRequiredParameters() {
        String body = """
                {
                  "resultCode": "00",
                  "resultMsg": "NORMAL SERVICE",
                  "content": {
                    "pageNo": 1,
                    "numOfRows": 100,
                    "totalCount": 1,
                    "items": [
                      {
                        "statsDt": "2026-07-14 10:00:00",
                        "aviSpotNm": "광안대교",
                        "lot": 129.1234,
                        "lat": 35.1234,
                        "vol": 1000
                      }
                    ]
                  }
                }
                """;
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        AviTrafficClient client = client(body, HttpStatus.OK, "encoded%2Bkey%2Fvalue%3D", capturedRequest);

        AviTrafficApiResponse response = client.fetch().block();

        assertThat(response).isNotNull();
        assertThat(response.content().items().get(0).get("aviSpotNm").stringValue()).isEqualTo("광안대교");
        String rawQuery = capturedRequest.get().url().getRawQuery();
        assertThat(capturedRequest.get().url().getPath()).isEqualTo("/6260000/BusanITSAVI/AVIList");
        assertThat(rawQuery).contains(
                "serviceKey=encoded%2Bkey%2Fvalue%3D",
                "pageNo=1",
                "numOfRows=100");
        assertThat(rawQuery).doesNotContain("%252B", "%252F", "%253D");
    }

    @Test
    void encodesDecodedServiceKeyExactlyOnce() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        AviTrafficClient client = client(successBody(), HttpStatus.OK, "decoded+key/value=", capturedRequest);

        client.fetch().block();

        assertThat(capturedRequest.get().url().getRawQuery())
                .contains("serviceKey=decoded%2Bkey%2Fvalue%3D")
                .doesNotContain("serviceKey=decoded+key/value=");
    }

    @Test
    void handlesXmlAuthenticationErrorReturnedWithHttp200() {
        String xmlError = """
                <OpenAPI_ServiceResponse>
                  <cmmMsgHeader>
                    <returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>
                  </cmmMsgHeader>
                </OpenAPI_ServiceResponse>
                """;
        AviTrafficClient client = client(xmlError, HttpStatus.OK, "test-key", new AtomicReference<>());

        assertThatThrownBy(() -> client.fetch().block())
                .isInstanceOf(AviTrafficAuthenticationException.class);
    }

    @Test
    void handlesProviderResultCodeFailure() {
        String body = """
                {"resultCode":"99","resultMsg":"PROVIDER ERROR","content":null}
                """;
        AviTrafficClient client = client(body, HttpStatus.OK, "test-key", new AtomicReference<>());

        assertThatThrownBy(() -> client.fetch().block())
                .isInstanceOf(AviTrafficApiException.class)
                .isNotInstanceOf(AviTrafficAuthenticationException.class);
    }

    @Test
    void handlesProviderAuthenticationResultCode() {
        String body = """
                {"resultCode":"101","resultMsg":"INVALID_REQUEST_APIM_KEY_ERROR","content":null}
                """;
        AviTrafficClient client = client(body, HttpStatus.OK, "test-key", new AtomicReference<>());

        assertThatThrownBy(() -> client.fetch().block())
                .isInstanceOf(AviTrafficAuthenticationException.class);
    }

    private AviTrafficClient client(
            String body,
            HttpStatus status,
            String serviceKey,
            AtomicReference<ClientRequest> capturedRequest
    ) {
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                    .body(body)
                    .build());
        };
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.getAvi().setServiceKey(serviceKey);
        properties.getAvi().setBaseUrl("https://apis.data.go.kr/6260000/BusanITSAVI");
        return new AviTrafficClient(
                WebClient.builder().exchangeFunction(exchangeFunction),
                new ObjectMapper(),
                properties
        );
    }

    private String successBody() {
        return """
                {"resultCode":"00","resultMsg":"NORMAL SERVICE","content":{"items":[]}}
                """;
    }
}
