package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.traffic.AviTrafficApiResponse;
import com.busantrip.exception.AviTrafficApiException;
import com.busantrip.exception.AviTrafficAuthenticationException;
import com.busantrip.exception.AviTrafficConfigurationException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
public class AviTrafficClient {

    private static final Pattern PERCENT_ESCAPE = Pattern.compile("%[0-9a-fA-F]{2}");
    private static final Set<String> SUCCESS_CODES = Set.of("0", "00");
    private static final Set<String> AUTHENTICATION_CODES = Set.of("20", "30", "31", "32", "101", "102");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ApiKeyProperties.Avi properties;

    public AviTrafficClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            ApiKeyProperties apiKeyProperties
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = apiKeyProperties.getAvi();
    }

    public Mono<AviTrafficApiResponse> fetch() {
        if (isBlank(properties.getServiceKey())) {
            return Mono.error(new AviTrafficConfigurationException("공공데이터포털 서비스 키가 설정되지 않았습니다."));
        }
        if (isBlank(properties.getBaseUrl())) {
            return Mono.error(new AviTrafficConfigurationException("AVI API Base URL이 설정되지 않았습니다."));
        }

        URI uri;
        try {
            uri = buildRequestUri();
        } catch (IllegalArgumentException exception) {
            return Mono.error(new AviTrafficConfigurationException("AVI API 설정이 올바르지 않습니다."));
        }

        return webClient.get()
                .uri(uri)
                .exchangeToMono(response -> handleResponse(response.statusCode(), response.bodyToMono(String.class)))
                .onErrorMap(WebClientRequestException.class,
                        error -> new AviTrafficApiException("AVI 교통량 API에 연결할 수 없습니다.", error));
    }

    URI buildRequestUri() {
        String decodedServiceKey = decodeServiceKeyOnce(properties.getServiceKey().trim());
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl().trim())
                .path("/AVIList")
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .encode(StandardCharsets.UTF_8)
                .buildAndExpand(Map.of("serviceKey", decodedServiceKey))
                .toUri();
    }

    private Mono<AviTrafficApiResponse> handleResponse(HttpStatusCode status, Mono<String> bodyMono) {
        return bodyMono.defaultIfEmpty("").flatMap(body -> {
            if (status.value() == 401 || status.value() == 403) {
                return Mono.error(new AviTrafficAuthenticationException("AVI 교통량 API 인증에 실패했습니다."));
            }
            if (!status.is2xxSuccessful()) {
                return Mono.error(new AviTrafficApiException("AVI 교통량 API 호출에 실패했습니다."));
            }
            return Mono.fromCallable(() -> parseBody(body));
        });
    }

    private AviTrafficApiResponse parseBody(String body) {
        String trimmedBody = body.trim();
        if (trimmedBody.isEmpty()) {
            throw new AviTrafficApiException("AVI 교통량 API가 빈 응답을 반환했습니다.");
        }
        if (trimmedBody.startsWith("<")) {
            if (containsAuthenticationError(trimmedBody)) {
                throw new AviTrafficAuthenticationException("AVI 교통량 API 인증에 실패했습니다.");
            }
            throw new AviTrafficApiException("AVI 교통량 API가 XML 오류 응답을 반환했습니다.");
        }

        AviTrafficApiResponse response;
        try {
            response = objectMapper.readValue(trimmedBody, AviTrafficApiResponse.class);
        } catch (Exception exception) {
            throw new AviTrafficApiException("AVI 교통량 API 응답을 해석할 수 없습니다.", exception);
        }

        String resultCode = response.resultCode() == null ? "" : response.resultCode().trim();
        if (!SUCCESS_CODES.contains(resultCode)) {
            if (AUTHENTICATION_CODES.contains(resultCode)
                    || containsAuthenticationError(response.resultMsg())) {
                throw new AviTrafficAuthenticationException("AVI 교통량 API 인증에 실패했습니다.");
            }
            throw new AviTrafficApiException("AVI 교통량 API가 실패 결과를 반환했습니다.");
        }
        return response;
    }

    private String decodeServiceKeyOnce(String serviceKey) {
        if (!PERCENT_ESCAPE.matcher(serviceKey).find()) {
            return serviceKey;
        }
        return UriUtils.decode(serviceKey, StandardCharsets.UTF_8);
    }

    private boolean containsAuthenticationError(String value) {
        if (value == null) {
            return false;
        }
        String upper = value.toUpperCase();
        return upper.contains("SERVICE_KEY")
                || upper.contains("SERVICE KEY")
                || upper.contains("AUTHENTICATION")
                || upper.contains("INVALID_REQUEST_APIM_KEY")
                || value.contains("인증키");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
