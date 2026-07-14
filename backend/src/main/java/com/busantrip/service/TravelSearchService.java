package com.busantrip.service;

import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.dto.travel.FailureProvider;
import com.busantrip.dto.travel.PartialFailure;
import com.busantrip.dto.travel.PlaceType;
import com.busantrip.dto.travel.TravelPlaceResponse;
import com.busantrip.dto.travel.TravelSearchResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TravelSearchService {

    private static final String TOURISM_FAILURE_MESSAGE = "관광지 정보를 불러오지 못했습니다.";
    private static final String RESTAURANT_FAILURE_MESSAGE = "음식점 정보를 불러오지 못했습니다.";

    private final LlmQueryService llmQueryService;
    private final NaverLocalSearchService naverLocalSearchService;

    public TravelSearchService(
            LlmQueryService llmQueryService,
            NaverLocalSearchService naverLocalSearchService
    ) {
        this.llmQueryService = llmQueryService;
        this.naverLocalSearchService = naverLocalSearchService;
    }

    public Mono<TravelSearchResponse> search(String message) {
        return llmQueryService.analyze(message)
                .flatMap(this::searchByCondition);
    }

    private Mono<TravelSearchResponse> searchByCondition(SearchCondition condition) {
        boolean searchTourism = switch (condition.intent()) {
            case TOURISM_SEARCH, COURSE_SEARCH -> true;
            case RESTAURANT_SEARCH -> false;
        };
        boolean searchRestaurant = switch (condition.intent()) {
            case RESTAURANT_SEARCH, COURSE_SEARCH -> true;
            case TOURISM_SEARCH -> false;
        };

        Mono<SearchOutcome> tourismOutcome = searchTourism
                ? searchProvider(
                        condition.tourismQuery(),
                        PlaceType.TOURISM,
                        FailureProvider.NAVER_TOURISM_SEARCH,
                        TOURISM_FAILURE_MESSAGE)
                : Mono.just(SearchOutcome.empty());
        Mono<SearchOutcome> restaurantOutcome = searchRestaurant
                ? searchProvider(
                        condition.restaurantQuery(),
                        PlaceType.RESTAURANT,
                        FailureProvider.NAVER_RESTAURANT_SEARCH,
                        RESTAURANT_FAILURE_MESSAGE)
                : Mono.just(SearchOutcome.empty());

        return Mono.zip(tourismOutcome, restaurantOutcome)
                .map(outcomes -> buildResponse(condition, outcomes.getT1(), outcomes.getT2()));
    }

    private Mono<SearchOutcome> searchProvider(
            String query,
            PlaceType type,
            FailureProvider provider,
            String failureMessage
    ) {
        if (query == null || query.isBlank()) {
            return Mono.just(SearchOutcome.failure(provider, failureMessage));
        }

        return naverLocalSearchService.search(query)
                .map(places -> new SearchOutcome(
                        places.stream().map(place -> toTravelPlace(place, type)).toList(),
                        List.of()
                ))
                .onErrorResume(error -> Mono.just(SearchOutcome.failure(provider, failureMessage)));
    }

    private TravelSearchResponse buildResponse(
            SearchCondition condition,
            SearchOutcome tourism,
            SearchOutcome restaurant
    ) {
        Map<String, TravelPlaceResponse> uniquePlaces = new LinkedHashMap<>();
        Stream.concat(tourism.places().stream(), restaurant.places().stream())
                .forEach(place -> uniquePlaces.putIfAbsent(deduplicationKey(place), place));
        List<PartialFailure> partialFailures = Stream.concat(
                        tourism.partialFailures().stream(),
                        restaurant.partialFailures().stream())
                .toList();

        return new TravelSearchResponse(
                responseMessage(condition),
                condition,
                List.copyOf(uniquePlaces.values()),
                partialFailures
        );
    }

    private TravelPlaceResponse toTravelPlace(PlaceResponse place, PlaceType type) {
        return new TravelPlaceResponse(
                place.name(),
                type,
                place.category(),
                place.address(),
                place.roadAddress(),
                place.latitude(),
                place.longitude(),
                place.link()
        );
    }

    private String deduplicationKey(TravelPlaceResponse place) {
        return "%s|%s|%s".formatted(
                place.name().strip().toLowerCase(Locale.ROOT),
                place.latitude(),
                place.longitude()
        );
    }

    private String responseMessage(SearchCondition condition) {
        String area = displayArea(condition.area());
        String tourismTopic = topic(condition.tourismQuery(), condition.area(), "관광지");
        String restaurantTopic = topic(condition.restaurantQuery(), condition.area(), "");
        String restaurantLabel = restaurantTopic.isBlank() ? "음식점" : restaurantTopic + " 음식점";

        return switch (condition.intent()) {
            case TOURISM_SEARCH -> "%s %s를 검색했습니다.".formatted(area, tourismTopic);
            case RESTAURANT_SEARCH -> "%s %s을 검색했습니다.".formatted(area, restaurantLabel);
            case COURSE_SEARCH -> "%s %s와 %s을 검색했습니다."
                    .formatted(area, tourismTopic, restaurantLabel);
        };
    }

    private String displayArea(String area) {
        if (area == null || area.isBlank()) {
            return "부산";
        }
        String withoutBusan = area.replaceFirst("^부산\\s*", "").strip();
        return withoutBusan.isBlank() ? "부산" : withoutBusan;
    }

    private String topic(String query, String area, String fallback) {
        if (query == null || query.isBlank()) {
            return fallback;
        }
        String topic = query;
        if (area != null && !area.isBlank()) {
            topic = topic.replaceFirst(Pattern.quote(area), "");
        }
        topic = topic.replaceFirst("^부산\\s*", "").strip();
        return topic.isBlank() ? fallback : topic;
    }

    private record SearchOutcome(
            List<TravelPlaceResponse> places,
            List<PartialFailure> partialFailures
    ) {
        private static SearchOutcome empty() {
            return new SearchOutcome(List.of(), List.of());
        }

        private static SearchOutcome failure(FailureProvider provider, String message) {
            return new SearchOutcome(List.of(), List.of(new PartialFailure(provider, message)));
        }
    }
}
