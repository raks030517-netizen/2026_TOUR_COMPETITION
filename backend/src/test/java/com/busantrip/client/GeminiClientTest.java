package com.busantrip.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.config.ApiKeyProperties;
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

class GeminiClientTest {

    @Test
    void 응답의_첫_후보_텍스트를_추출한다() {
        String rawJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "부산 조용한 카페"}
                        ],
                        "role": "model"
                      }
                    }
                  ]
                }
                """;

        GeminiClient client = buildClientReturning(HttpStatus.OK, rawJson);

        StepVerifier.create(client.generate("아무 프롬프트"))
                .expectNext("부산 조용한 카페")
                .verifyComplete();
    }

    @Test
    void 인증키가_없으면_API를_호출하지_않고_바로_에러를_반환한다() {
        GeminiClient client = new GeminiClient(WebClient.builder(), new ApiKeyProperties());

        StepVerifier.create(client.generate("아무 프롬프트"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    // 2026-07-15 실제로 겪은 "무료 티어 할당량 0" 429 응답 형태를 그대로 재현한 회귀 테스트.
    @Test
    void 요청한도_초과응답이면_상태코드를_담은_에러를_반환한다() {
        String errorBody = """
                {"error": {"code": 429, "status": "RESOURCE_EXHAUSTED", "message": "quota exceeded"}}
                """;

        GeminiClient client = buildClientReturning(HttpStatus.TOO_MANY_REQUESTS, errorBody);

        StepVerifier.create(client.generate("아무 프롬프트"))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("429"))
                .verify();
    }

    private GeminiClient buildClientReturning(HttpStatus status, String responseBody) {
        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse clientResponse = ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(responseBody)
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(clientResponse));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);

        ApiKeyProperties properties = new ApiKeyProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setModel("gemini-2.0-flash");

        return new GeminiClient(builder, properties);
    }
}
