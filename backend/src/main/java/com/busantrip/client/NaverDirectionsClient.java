package com.busantrip.client;

import com.busantrip.config.NaverMapsProperties;
import com.busantrip.dto.external.route.NaverDirectionsResponse;
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
public class NaverDirectionsClient {

    private static final String DRIVING_PATH = "/driving";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final NaverMapsProperties properties;

    public NaverDirectionsClient(
            WebClient.Builder webClientBuilder,
            NaverMapsProperties properties
    ) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .build();
    }

    public Mono<NaverDirectionsResponse> getDrivingRoute(
            double startLongitude,
            double startLatitude,
            double endLongitude,
            double endLatitude,
            String option
    ) {
        validateConfiguration();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DRIVING_PATH)
                        .queryParam("start", coordinate(startLongitude, startLatitude))
                        .queryParam("goal", coordinate(endLongitude, endLatitude))
                        .queryParam("option", option)
                        .queryParam("lang", "ko")
                        .build())
                .header("x-ncp-apigw-api-key-id", properties.getKeyId().trim())
                .header("x-ncp-apigw-api-key", properties.getKey().trim())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(NaverDirectionsResponse.class);
                    }
                    return response.releaseBody()
                            .then(Mono.error(toApiException(response.statusCode())));
                })
                .timeout(TIMEOUT)
                .onErrorMap(
                        TimeoutException.class,
                        error -> new RouteApiException(
                                "ROUTE_API_TIMEOUT",
                                "자동차 경로 조회 시간이 초과되었습니다.",
                                HttpStatus.GATEWAY_TIMEOUT,
                                error)
                )
                .onErrorMap(
                        WebClientRequestException.class,
                        error -> new RouteApiException(
                                "ROUTE_API_UNAVAILABLE",
                                "자동차 경로 서비스에 연결할 수 없습니다.",
                                HttpStatus.BAD_GATEWAY,
                                error)
                );
    }

    private void validateConfiguration() {
        if (isBlank(properties.getKeyId()) || isBlank(properties.getKey())) {
            throw new RouteApiException(
                    "ROUTE_CONFIGURATION_ERROR",
                    "네이버 Maps 서버 API 설정을 확인해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private RouteApiException toApiException(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403) {
            return new RouteApiException(
                    "NAVER_MAPS_AUTHENTICATION_FAILED",
                    "네이버 Maps API 인증에 실패했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        }
        if (status.value() == 429) {
            return new RouteApiException(
                    "NAVER_MAPS_QUOTA_EXCEEDED",
                    "네이버 Maps API 사용량 한도를 확인해 주세요.",
                    HttpStatus.BAD_GATEWAY
            );
        }
        return new RouteApiException(
                "ROUTE_API_UNAVAILABLE",
                "자동차 경로 서비스가 요청을 처리하지 못했습니다.",
                HttpStatus.BAD_GATEWAY
        );
    }

    private String coordinate(double longitude, double latitude) {
        return longitude + "," + latitude;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) {
            return "https://maps.apigw.ntruss.com/map-direction/v1";
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
