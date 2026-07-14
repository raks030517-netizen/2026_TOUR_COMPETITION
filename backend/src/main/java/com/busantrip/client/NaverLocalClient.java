package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.place.NaverLocalApiResponse;
import com.busantrip.exception.NaverLocalApiException;
import com.busantrip.exception.NaverLocalAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class NaverLocalClient {

    private static final String BASE_URL = "https://openapi.naver.com";

    private final WebClient webClient;
    private final ApiKeyProperties.Naver.Search credentials;

    public NaverLocalClient(WebClient.Builder webClientBuilder, ApiKeyProperties apiKeyProperties) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.credentials = apiKeyProperties.getNaver().getSearch();
    }

    public Mono<NaverLocalApiResponse> search(String query) {
        if (isBlank(credentials.getClientId()) || isBlank(credentials.getClientSecret())) {
            return Mono.error(new NaverLocalAuthenticationException("네이버 지역 검색 API 인증 정보가 설정되지 않았습니다."));
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/local.json")
                        .queryParam("query", query)
                        .queryParam("display", 5)
                        .queryParam("start", 1)
                        .queryParam("sort", "random")
                        .build())
                .header("X-Naver-Client-Id", credentials.getClientId())
                .header("X-Naver-Client-Secret", credentials.getClientSecret())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(NaverLocalApiResponse.class)
                                .defaultIfEmpty(NaverLocalApiResponse.empty());
                    }
                    if (response.statusCode().value() == 401 || response.statusCode().value() == 403) {
                        return response.releaseBody().then(Mono.error(
                                new NaverLocalAuthenticationException("네이버 지역 검색 API 인증에 실패했습니다.")));
                    }
                    return response.releaseBody().then(Mono.error(
                            new NaverLocalApiException("네이버 지역 검색 API 호출에 실패했습니다.")));
                })
                .onErrorMap(WebClientRequestException.class,
                        error -> new NaverLocalApiException("네이버 지역 검색 API에 연결할 수 없습니다.", error));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
