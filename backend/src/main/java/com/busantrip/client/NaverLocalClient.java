package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NaverLocalClient {

    private static final String LOCAL_SEARCH_PATH = "/v1/search/local.json";

    // 네이버 지역검색 API는 한 요청당 최대 5건까지만 돌려준다.
    private static final int MAX_DISPLAY = 5;

    // mapx/mapy는 WGS84 좌표에 10^7을 곱한 정수 문자열로 내려온다.
    private static final double COORDINATE_SCALE = 1e7;

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public NaverLocalClient(WebClient.Builder webClientBuilder, ApiKeyProperties apiKeyProperties) {
        this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com")
                .build();

        ApiKeyProperties.Naver.Search search = apiKeyProperties.getNaver().getSearch();
        this.clientId = search.getClientId().trim();
        this.clientSecret = search.getClientSecret().trim();
    }

    /**
     * 네이버 지역검색으로 장소를 찾아 우리 응답 모델로 변환해 돌려준다.
     */
    public Mono<List<PlaceResponse>> search(String query) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Mono.error(new IllegalStateException("네이버 지역검색 API 인증키가 설정되지 않았습니다."));
        }
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("검색어는 비어 있을 수 없습니다."));
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOCAL_SEARCH_PATH)
                        .queryParam("query", "{query}")
                        .queryParam("display", MAX_DISPLAY)
                        .build(query)
                )
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("응답 본문 없음")
                                .flatMap(errorBody -> Mono.error(
                                        new IllegalStateException(
                                                "네이버 지역검색 API 호출 실패: "
                                                        + response.statusCode()
                                                        + ", 응답: " + errorBody
                                        )
                                ))
                )
                .bodyToMono(LocalSearchResponse.class)
                .map(NaverLocalClient::toPlaceResponses);
    }

    private static List<PlaceResponse> toPlaceResponses(LocalSearchResponse response) {
        return response.items().stream()
                .map(NaverLocalClient::toPlaceResponse)
                .toList();
    }

    private static PlaceResponse toPlaceResponse(LocalSearchItem item) {
        String address = item.roadAddress() != null && !item.roadAddress().isBlank()
                ? item.roadAddress()
                : item.address();

        return new PlaceResponse(
                stripHtmlTags(item.title()),
                address,
                parseCoordinate(item.mapy()),
                parseCoordinate(item.mapx())
        );
    }

    private static String stripHtmlTags(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "");
    }

    private static Double parseCoordinate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return Double.parseDouble(rawValue) / COORDINATE_SCALE;
    }

    private record LocalSearchResponse(List<LocalSearchItem> items) {
    }

    private record LocalSearchItem(
            String title,
            String address,
            String roadAddress,
            String mapx,
            String mapy
    ) {
    }
}
