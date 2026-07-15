package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.exception.GemmaApiException;
import com.busantrip.exception.GemmaAuthenticationException;
import com.busantrip.exception.GemmaConfigurationException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class GemmaClient {

    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    private final WebClient webClient;
    private final ApiKeyProperties.Gemma settings;

    public GemmaClient(WebClient.Builder webClientBuilder, ApiKeyProperties apiKeyProperties) {
        this.settings = apiKeyProperties.getGemma();
        this.webClient = webClientBuilder.baseUrl(normalizeBaseUrl(settings.getBaseUrl())).build();
    }

    public Mono<String> generate(String systemInstruction, String userPrompt) {
        String apiKey = requiredSetting(settings.getApiKey(), "Gemma API 키가 설정되지 않았습니다.");
        String model = normalizeModelName(requiredSetting(settings.getModel(), "Gemma 모델이 설정되지 않았습니다."));
        GenerateContentRequest request = new GenerateContentRequest(
                List.of(new RequestContent("user", List.of(new RequestPart(userPrompt)))),
                new SystemInstruction(List.of(new RequestPart(systemInstruction)))
        );

        return webClient.post()
                .uri("/models/" + model + ":generateContent")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", apiKey)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(GenerateContentResponse.class)
                                .map(GenerateContentResponse::text);
                    }
                    if (response.statusCode().value() == 401 || response.statusCode().value() == 403) {
                        return response.releaseBody().then(Mono.error(
                                new GemmaAuthenticationException("Gemma API 인증에 실패했습니다.")));
                    }
                    return response.releaseBody().then(Mono.error(
                            new GemmaApiException("Gemma API 호출에 실패했습니다.")));
                })
                .onErrorMap(WebClientRequestException.class,
                        error -> new GemmaApiException("Gemma API에 연결할 수 없습니다.", error));
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (value.isEmpty()) {
            throw new GemmaConfigurationException("Gemma API 기본 URL이 설정되지 않았습니다.");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String requiredSetting(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new GemmaConfigurationException(message);
        }
        return value.trim();
    }

    private String normalizeModelName(String configuredModel) {
        String model = configuredModel.startsWith("models/")
                ? configuredModel.substring("models/".length())
                : configuredModel;
        if (!MODEL_NAME_PATTERN.matcher(model).matches()) {
            throw new GemmaConfigurationException("Gemma 모델 이름 형식이 올바르지 않습니다.");
        }
        return model;
    }

    private record GenerateContentRequest(
            List<RequestContent> contents,
            SystemInstruction systemInstruction
    ) {
    }

    private record RequestContent(String role, List<RequestPart> parts) {
    }

    private record SystemInstruction(List<RequestPart> parts) {
    }

    private record RequestPart(String text) {
    }

    private record GenerateContentResponse(List<Candidate> candidates) {

        private String text() {
            if (candidates == null || candidates.isEmpty() || candidates.getFirst().content() == null) {
                throw new GemmaApiException("Gemma API 응답에 분석 결과가 없습니다.");
            }
            List<ResponsePart> parts = candidates.getFirst().content().parts();
            if (parts == null) {
                throw new GemmaApiException("Gemma API 응답에 분석 결과가 없습니다.");
            }
            String text = parts.stream()
                    .map(ResponsePart::text)
                    .filter(Objects::nonNull)
                    .reduce("", String::concat)
                    .trim();
            if (text.isEmpty()) {
                throw new GemmaApiException("Gemma API 응답에 분석 결과가 없습니다.");
            }
            return text;
        }
    }

    private record Candidate(ResponseContent content) {
    }

    private record ResponseContent(List<ResponsePart> parts) {
    }

    private record ResponsePart(String text) {
    }
}
