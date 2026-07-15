package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class GeminiClient {

    private static final String GENERATE_CONTENT_PATH = "/v1beta/models/{model}:generateContent";

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(WebClient.Builder webClientBuilder, ApiKeyProperties apiKeyProperties) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        ApiKeyProperties.Gemini gemini = apiKeyProperties.getGemini();
        this.apiKey = gemini.getApiKey().trim();
        this.model = gemini.getModel();
    }

    /**
     * 주어진 프롬프트로 Gemini에 텍스트 생성을 요청하고, 응답의 첫 후보 텍스트만 돌려준다.
     */
    public Mono<String> generate(String prompt) {
        if (apiKey.isBlank()) {
            return Mono.error(new IllegalStateException("Gemini API 인증키가 설정되지 않았습니다."));
        }

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(GENERATE_CONTENT_PATH)
                        .queryParam("key", "{apiKey}")
                        .build(model, apiKey)
                )
                .bodyValue(GenerateContentRequest.of(prompt))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("응답 본문 없음")
                                .flatMap(errorBody -> Mono.error(
                                        new IllegalStateException(
                                                "Gemini API 호출 실패: "
                                                        + response.statusCode()
                                                        + ", 응답: " + errorBody
                                        )
                                ))
                )
                .bodyToMono(GenerateContentResponse.class)
                .map(GeminiClient::extractText);
    }

    private static String extractText(GenerateContentResponse response) {
        return response.candidates().stream()
                .findFirst()
                .map(Candidate::content)
                .flatMap(content -> content.parts().stream().findFirst())
                .map(Part::text)
                .orElseThrow(() -> new IllegalStateException("Gemini 응답에서 텍스트를 찾을 수 없습니다."));
    }

    private record GenerateContentRequest(List<Content> contents) {
        static GenerateContentRequest of(String prompt) {
            return new GenerateContentRequest(List.of(new Content(List.of(new Part(prompt)))));
        }
    }

    private record GenerateContentResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}
