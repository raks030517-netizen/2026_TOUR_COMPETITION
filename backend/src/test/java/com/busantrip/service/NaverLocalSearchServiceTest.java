package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.client.NaverLocalClient;
import com.busantrip.dto.place.NaverLocalApiResponse;
import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class NaverLocalSearchServiceTest {

    private final NaverLocalClient client = mock(NaverLocalClient.class);
    private final NaverLocalSearchService service = new NaverLocalSearchService(client);

    @Test
    void removesTitleHtmlAndConvertsWgs84IntegerCoordinates() {
        NaverLocalApiResponse.Item item = new NaverLocalApiResponse.Item(
                "<b>해운대</b>&amp;송정",
                "https://example.com/place",
                "여행,명소>해수욕장",
                "부산광역시 해운대구 우동",
                "부산광역시 해운대구 해운대해변로 264",
                1_291_603_840L,
                351_593_251L
        );
        when(client.search("부산 해운대 관광지"))
                .thenReturn(Mono.just(new NaverLocalApiResponse(List.of(item))));

        List<PlaceResponse> result = service.search("부산 해운대 관광지").block();

        assertThat(result).containsExactly(new PlaceResponse(
                "해운대&송정",
                "여행,명소>해수욕장",
                "부산광역시 해운대구 우동",
                "부산광역시 해운대구 해운대해변로 264",
                35.1593251,
                129.160384,
                "https://example.com/place"
        ));
    }

    @Test
    void returnsEmptyListWhenNaverHasNoResults() {
        when(client.search("검색 결과 없음"))
                .thenReturn(Mono.just(NaverLocalApiResponse.empty()));

        List<PlaceResponse> result = service.search("검색 결과 없음").block();

        assertThat(result).isEmpty();
    }
}
