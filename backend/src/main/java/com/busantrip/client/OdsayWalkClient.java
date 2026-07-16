package com.busantrip.client;

import com.busantrip.config.OdsayProperties;
import com.busantrip.dto.external.route.OdsayWalkResponse;
import com.busantrip.exception.RouteApiException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class OdsayWalkClient {

    private static final String WALK_PATH = "/searchWalkPathV2";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;
    private final OdsayProperties properties;

    public OdsayWalkClient(WebClient.Builder webClientBuilder, OdsayProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .build();
    }

    public Mono<OdsayWalkResponse> findRoute(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude
    ) {
        validateConfiguration();
        String locations = startLongitude + "," + startLatitude + ","
                + endLongitude + "," + endLatitude;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(WALK_PATH)
                        .queryParam("opt", "reco")
                        .queryParam("loc", locations)
                        .queryParam("lang", 0)
                        .queryParam("output", "json")
                        // URI 변수로 확장해야 API Key의 '+', '/', '='가 쿼리 값으로 인코딩된다.
                        .queryParam("apiKey", "{apiKey}")
                        .build(properties.getKey().trim()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(OdsayWalkResponse.class);
                    }
                    return response.releaseBody()
                            .then(Mono.error(httpException(response.statusCode())));
                })
                .timeout(TIMEOUT)
                .onErrorMap(
                        TimeoutException.class,
                        error -> new RouteApiException(
                                "WALK_API_TIMEOUT",
                                "도보 경로 조회 시간이 초과되었습니다.",
                                HttpStatus.GATEWAY_TIMEOUT))
                .onErrorMap(
                        WebClientRequestException.class,
                        error -> new RouteApiException(
                                "WALK_API_UNAVAILABLE",
                                "도보 경로 서비스에 연결할 수 없습니다.",
                                HttpStatus.BAD_GATEWAY));
    }

    private void validateConfiguration() {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            throw new RouteApiException(
                    "WALK_CONFIGURATION_ERROR",
                    "ODsay 서버 API 설정을 확인해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private RouteApiException httpException(HttpStatusCode status) {
        if (status.value() == 401) {
            return new RouteApiException(
                    "ODSAY_AUTHENTICATION_FAILED",
                    "ODsay API Key와 등록된 서버 IP를 확인해 주세요.",
                    HttpStatus.BAD_GATEWAY);
        }
        if (status.value() == 403 || status.value() == 404) {
            return unavailable();
        }
        if (status.value() == 429) {
            return new RouteApiException(
                    "ODSAY_QUOTA_EXCEEDED",
                    "ODsay API 사용량 한도를 확인해 주세요.",
                    HttpStatus.BAD_GATEWAY);
        }
        return unavailable();
    }

    private RouteApiException unavailable() {
        return new RouteApiException(
                "WALK_API_UNAVAILABLE",
                "현재 ODsay 상품 또는 권한으로 도보 길찾기를 사용할 수 없습니다.",
                HttpStatus.BAD_GATEWAY);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.odsay.com/v1/api";
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }
}
