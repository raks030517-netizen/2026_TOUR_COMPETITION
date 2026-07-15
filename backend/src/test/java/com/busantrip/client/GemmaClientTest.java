package com.busantrip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.exception.GemmaAuthenticationException;
import com.busantrip.exception.GlobalExceptionHandler;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class GemmaClientTest {

    private static final String TEST_API_KEY = "test-gemma-secret-key";

    @Test
    void mapsGeneratedTextAndUsesConfiguredModel() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body("""
                            {
                              "candidates": [{
                                "content": {
                                  "parts": [{"text":"{\\"intent\\":\\"TOURISM_SEARCH\\"}"}]
                                }
                              }]
                            }
                            """)
                    .build());
        };
        GemmaClient client = client(exchangeFunction);

        String result = client.generate("system", "user").block();

        assertThat(result).isEqualTo("{\"intent\":\"TOURISM_SEARCH\"}");
        assertThat(capturedRequest.get().url().getPath())
                .isEqualTo("/v1beta/models/test-gemma-model:generateContent");
        assertThat(capturedRequest.get().headers().getFirst("x-goog-api-key"))
                .isEqualTo(TEST_API_KEY);
    }

    @Test
    void doesNotExposeApiKeyInErrorsOrPublicErrorResponse() {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.UNAUTHORIZED).build());
        GemmaClient client = client(exchangeFunction);

        Throwable error = catchThrowable(() -> client.generate("system", "user").block());
        String publicResponse = new GlobalExceptionHandler()
                .handleGemmaAuthenticationException()
                .toString();

        assertThat(error).isInstanceOf(GemmaAuthenticationException.class);
        assertThat(error.getMessage()).doesNotContain(TEST_API_KEY);
        assertThat(publicResponse).doesNotContain(TEST_API_KEY);
    }

    private GemmaClient client(ExchangeFunction exchangeFunction) {
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.getGemma().setApiKey(TEST_API_KEY);
        properties.getGemma().setModel("test-gemma-model");
        properties.getGemma().setBaseUrl("https://example.test/v1beta");
        return new GemmaClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);
    }
}
