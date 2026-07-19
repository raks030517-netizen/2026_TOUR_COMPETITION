package com.busantrip.service;

import com.busantrip.client.LocalTourismClient;
import com.busantrip.client.RelatedTourismClient;
import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.external.tourism.LocalTourismApiResponse;
import com.busantrip.dto.external.tourism.RelatedTourismApiResponse;
import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.dto.response.TourismResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TourismService {

    private final LocalTourismClient localTourismClient;
    private final RelatedTourismClient relatedTourismClient;
    private final CategoryKeywordResolver categoryKeywordResolver;
    private final DemoTourismCatalog demoTourismCatalog;
    private final ApiKeyProperties apiKeyProperties;

    public TourismService(
            LocalTourismClient localTourismClient,
            RelatedTourismClient relatedTourismClient,
            CategoryKeywordResolver categoryKeywordResolver,
            DemoTourismCatalog demoTourismCatalog,
            ApiKeyProperties apiKeyProperties
    ) {
        this.localTourismClient = localTourismClient;
        this.relatedTourismClient = relatedTourismClient;
        this.categoryKeywordResolver = categoryKeywordResolver;
        this.demoTourismCatalog = demoTourismCatalog;
        this.apiKeyProperties = apiKeyProperties;
    }

    public Mono<TourismResponse> getLocalTourism(
            String baseYm,
            String signguCd,
            int pageNo,
            int numOfRows
    ) {
        if (!hasTourismApiKey()) {
            return Mono.just(demoTourismCatalog.search("추천", pageNo, numOfRows));
        }

        return localTourismClient.getLocalTourism(baseYm, signguCd, pageNo, numOfRows)
                .map(this::toLocalTourismResponse)
                .onErrorResume(error -> Mono.just(demoTourismCatalog.search("추천", pageNo, numOfRows)));
    }

    public Mono<TourismResponse> getRelatedTourismByArea(
            String baseYm,
            String signguCd,
            int pageNo,
            int numOfRows
    ) {
        if (!hasTourismApiKey()) {
            return Mono.just(demoTourismCatalog.search("추천", pageNo, numOfRows));
        }

        return relatedTourismClient.getRelatedTourismByArea(baseYm, signguCd, pageNo, numOfRows)
                .map(this::toRelatedTourismResponse)
                .onErrorResume(error -> Mono.just(demoTourismCatalog.search("추천", pageNo, numOfRows)));
    }

    /**
     * 관련 관광지 API는 좌표를 내려주지 않아 지도에 바로 그릴 수 없다. 검색 화면에서는
     * 좌표를 제공하는 기초지자체 관광지 API를 우선 사용하고, 키가 없거나 호출에 실패하면
     * 발표용 카탈로그를 반환한다.
     */
    public Mono<TourismResponse> searchRelatedTourism(
            String baseYm,
            String signguCd,
            String keyword,
            int pageNo,
            int numOfRows
    ) {
        String resolvedKeyword = categoryKeywordResolver.resolve(keyword);
        if (!hasTourismApiKey()) {
            return Mono.just(demoTourismCatalog.search(resolvedKeyword, pageNo, numOfRows));
        }

        return localTourismClient.getLocalTourism(baseYm, signguCd, pageNo, Math.max(numOfRows, 20))
                .map(this::toLocalTourismResponse)
                .map(response -> filterForKeyword(response, resolvedKeyword, pageNo, numOfRows))
                .onErrorResume(error -> Mono.just(demoTourismCatalog.search(resolvedKeyword, pageNo, numOfRows)));
    }

    private TourismResponse toLocalTourismResponse(LocalTourismApiResponse apiResponse) {
        LocalTourismApiResponse.Body body = apiResponse.response().body();
        List<LocalTourismApiResponse.Item> items = body.items() == null || body.items().item() == null
                ? List.of()
                : body.items().item();

        List<PlaceResponse> places = items.stream()
                .map(item -> new PlaceResponse(
                        item.hubTatsNm(),
                        item.signguNm(),
                        item.hubCtgryLclsNm(),
                        item.hubCtgryMclsNm(),
                        parseDouble(item.mapY()),
                        parseDouble(item.mapX()),
                        parseInteger(item.hubRank())
                ))
                .filter(place -> place.latitude() != null && place.longitude() != null)
                .toList();

        return new TourismResponse(
                numberOrZero(body.totalCount()),
                numberOrZero(body.pageNo()),
                numberOrZero(body.numOfRows()),
                places
        );
    }

    private TourismResponse toRelatedTourismResponse(RelatedTourismApiResponse apiResponse) {
        RelatedTourismApiResponse.Body body = apiResponse.response().body();
        List<RelatedTourismApiResponse.Item> items = body.items() == null || body.items().item() == null
                ? List.of()
                : body.items().item();

        List<PlaceResponse> places = items.stream()
                .map(item -> new PlaceResponse(
                        item.rlteTatsNm(),
                        item.rlteSignguNm(),
                        item.rlteCtgryLclsNm(),
                        item.rlteCtgryMclsNm(),
                        null,
                        null,
                        parseInteger(item.rlteRank())
                ))
                .toList();

        return new TourismResponse(
                numberOrZero(body.totalCount()),
                numberOrZero(body.pageNo()),
                numberOrZero(body.numOfRows()),
                places
        );
    }

    private TourismResponse filterForKeyword(TourismResponse response, String keyword, int pageNo, int numOfRows) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        List<PlaceResponse> matches = response.places().stream()
                .filter(place -> searchable(place).contains(normalized))
                .toList();
        List<PlaceResponse> selected = matches.isEmpty() ? response.places() : matches;
        int safeRows = Math.max(1, Math.min(numOfRows, 20));
        return new TourismResponse(
                selected.size(),
                Math.max(pageNo, 1),
                safeRows,
                selected.stream().limit(safeRows).toList()
        );
    }

    private String searchable(PlaceResponse place) {
        return String.join(" ", value(place.name()), value(place.district()), value(place.category()), value(place.subCategory()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean hasTourismApiKey() {
        String serviceKey = apiKeyProperties.getTourism().getServiceKey();
        return serviceKey != null && !serviceKey.isBlank();
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int numberOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String value(String input) {
        return input == null ? "" : input;
    }
}
