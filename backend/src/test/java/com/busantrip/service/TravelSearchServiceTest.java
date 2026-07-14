package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.dto.llm.SearchIntent;
import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.dto.travel.FailureProvider;
import com.busantrip.dto.travel.PlaceType;
import com.busantrip.dto.travel.TravelSearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class TravelSearchServiceTest {

    private final LlmQueryService llmQueryService = mock(LlmQueryService.class);
    private final NaverLocalSearchService naverLocalSearchService = mock(NaverLocalSearchService.class);
    private final TravelSearchService service = new TravelSearchService(
            llmQueryService, naverLocalSearchService);

    @Test
    void tourismIntentCallsOnlyTourismSearch() {
        SearchCondition condition = condition(
                SearchIntent.TOURISM_SEARCH,
                "부산 해운대 관광지",
                "부산 해운대 맛집");
        when(llmQueryService.analyze("해운대 관광지를 추천해 주세요."))
                .thenReturn(Mono.just(condition));
        when(naverLocalSearchService.search("부산 해운대 관광지"))
                .thenReturn(Mono.just(List.of(place("해운대해수욕장", 35.1587, 129.1604))));

        TravelSearchResponse response = service.search("해운대 관광지를 추천해 주세요.").block();

        assertThat(response).isNotNull();
        assertThat(response.places()).hasSize(1);
        assertThat(response.places().getFirst().type()).isEqualTo(PlaceType.TOURISM);
        verify(naverLocalSearchService).search("부산 해운대 관광지");
        verify(naverLocalSearchService, never()).search("부산 해운대 맛집");
    }

    @Test
    void restaurantIntentCallsOnlyRestaurantSearch() {
        SearchCondition condition = condition(
                SearchIntent.RESTAURANT_SEARCH,
                "부산 서면 관광지",
                "부산 서면 돼지국밥");
        when(llmQueryService.analyze("서면에서 돼지국밥을 먹고 싶어요."))
                .thenReturn(Mono.just(condition));
        when(naverLocalSearchService.search("부산 서면 돼지국밥"))
                .thenReturn(Mono.just(List.of(place("돼지국밥집", 35.1578, 129.0592))));

        TravelSearchResponse response = service.search("서면에서 돼지국밥을 먹고 싶어요.").block();

        assertThat(response).isNotNull();
        assertThat(response.places().getFirst().type()).isEqualTo(PlaceType.RESTAURANT);
        verify(naverLocalSearchService).search("부산 서면 돼지국밥");
        verify(naverLocalSearchService, never()).search("부산 서면 관광지");
    }

    @Test
    void courseIntentCombinesTypesAndRemovesDuplicatePlaces() {
        SearchCondition condition = condition(
                SearchIntent.COURSE_SEARCH,
                "부산 광안리 관광지",
                "부산 광안리 조개구이");
        PlaceResponse duplicate = place("광안리 공통 장소", 35.1532, 129.1187);
        when(llmQueryService.analyze("광안리에서 바다를 보고 조개구이를 먹고 싶어요."))
                .thenReturn(Mono.just(condition));
        when(naverLocalSearchService.search("부산 광안리 관광지"))
                .thenReturn(Mono.just(List.of(
                        duplicate,
                        place("광안리해수욕장", 35.1531, 129.1186))));
        when(naverLocalSearchService.search("부산 광안리 조개구이"))
                .thenReturn(Mono.just(List.of(
                        duplicate,
                        place("광안리 조개구이", 35.1540, 129.1200))));

        TravelSearchResponse response = service.search(
                "광안리에서 바다를 보고 조개구이를 먹고 싶어요.").block();

        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("광안리 관광지와 조개구이 음식점을 검색했습니다.");
        assertThat(response.places()).hasSize(3);
        assertThat(response.places())
                .extracting(place -> place.type())
                .contains(PlaceType.TOURISM, PlaceType.RESTAURANT);
        assertThat(response.places().stream()
                .filter(place -> place.name().equals("광안리 공통 장소")))
                .hasSize(1);
        assertThat(response.partialFailures()).isEmpty();
    }

    @Test
    void returnsTourismResultsWhenRestaurantSearchFails() {
        SearchCondition condition = condition(
                SearchIntent.COURSE_SEARCH,
                "부산 광안리 관광지",
                "부산 광안리 조개구이");
        when(llmQueryService.analyze("광안리 코스"))
                .thenReturn(Mono.just(condition));
        when(naverLocalSearchService.search("부산 광안리 관광지"))
                .thenReturn(Mono.just(List.of(place("광안리해수욕장", 35.1531, 129.1186))));
        when(naverLocalSearchService.search("부산 광안리 조개구이"))
                .thenReturn(Mono.error(new RuntimeException("upstream failure")));

        TravelSearchResponse response = service.search("광안리 코스").block();

        assertThat(response).isNotNull();
        assertThat(response.places()).hasSize(1);
        assertThat(response.partialFailures()).hasSize(1);
        assertThat(response.partialFailures().getFirst().provider())
                .isEqualTo(FailureProvider.NAVER_RESTAURANT_SEARCH);
        assertThat(response.partialFailures().getFirst().message())
                .isEqualTo("음식점 정보를 불러오지 못했습니다.");
    }

    private SearchCondition condition(
            SearchIntent intent,
            String tourismQuery,
            String restaurantQuery
    ) {
        return new SearchCondition(
                intent,
                intent == SearchIntent.RESTAURANT_SEARCH ? "부산 서면"
                        : intent == SearchIntent.TOURISM_SEARCH ? "부산 해운대" : "부산 광안리",
                tourismQuery,
                restaurantQuery,
                false,
                false,
                false
        );
    }

    private PlaceResponse place(String name, double latitude, double longitude) {
        return new PlaceResponse(
                name,
                "여행,명소",
                "부산광역시 주소",
                "부산광역시 도로명주소",
                latitude,
                longitude,
                "https://example.com/place"
        );
    }
}
