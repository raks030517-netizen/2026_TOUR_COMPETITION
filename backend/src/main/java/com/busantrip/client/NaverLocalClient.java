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
        // 네이버는 순위 정보를 내려주지 않는다 — 다른 팀원 코드(TourismService)처럼 없는 값은 null로 정직하게 둔다.
        String[] categoryParts = splitCategory(item.category());

        return new PlaceResponse(
                stripHtmlTags(item.title()),
                extractDistrict(item.address()),
                categoryParts[0],
                categoryParts[1],
                parseCoordinate(item.mapy()),
                parseCoordinate(item.mapx()),
                null
        );
    }

    private static String stripHtmlTags(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "");
    }

    // 네이버 주소는 "시도 시군구 ..." 순서로 내려온다 (예: "부산광역시 해운대구 우동 1408").
    private static String extractDistrict(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String[] tokens = address.trim().split("\\s+");
        return tokens.length > 1 ? tokens[1] : null;
    }

    // 네이버 category는 "대분류>중분류" 형식으로 내려온다 (예: "음식점>카페,디저트").
    private static String[] splitCategory(String category) {
        if (category == null || category.isBlank()) {
            return new String[] {null, null};
        }
        String[] parts = category.split(">", 2);
        String large = parts[0].isBlank() ? null : parts[0];
        String mid = parts.length > 1 && !parts[1].isBlank() ? parts[1] : null;
        return new String[] {large, mid};
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
            String category,
            String address,
            String roadAddress,
            String mapx,
            String mapy
    ) {
    }
}
