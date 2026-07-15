package com.busantrip.service;

import static org.mockito.Mockito.when;

import com.busantrip.dto.request.ChatRequest;
import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TravelChatServiceTest {

    @Mock
    private LlmQueryService llmQueryService;

    @Mock
    private NaverLocalSearchService naverLocalSearchService;

    @Test
    void 검색결과가_있으면_장소개수를_포함한_안내문구를_돌려준다() {
        when(llmQueryService.analyze("부산 조용한 카페 추천해줘")).thenReturn(Mono.just("부산 조용한 카페"));
        List<PlaceResponse> places = List.of(
                new PlaceResponse("카페1", "해운대구", "음식점", "카페,디저트", 35.1, 129.1, null),
                new PlaceResponse("카페2", "수영구", "음식점", "카페,디저트", 35.2, 129.2, null)
        );
        when(naverLocalSearchService.search("부산 조용한 카페")).thenReturn(Mono.just(places));

        TravelChatService service = new TravelChatService(llmQueryService, naverLocalSearchService);

        StepVerifier.create(service.chat(new ChatRequest("부산 조용한 카페 추천해줘")))
                .expectNextMatches(response ->
                        response.places().equals(places)
                                && response.message().contains("2곳"))
                .verifyComplete();
    }

    @Test
    void 검색결과가_없으면_못찾았다는_안내문구를_돌려준다() {
        when(llmQueryService.analyze("이상한 요청")).thenReturn(Mono.just("이상한 검색어"));
        when(naverLocalSearchService.search("이상한 검색어")).thenReturn(Mono.just(List.of()));

        TravelChatService service = new TravelChatService(llmQueryService, naverLocalSearchService);

        StepVerifier.create(service.chat(new ChatRequest("이상한 요청")))
                .expectNextMatches(response ->
                        response.places().isEmpty()
                                && response.message().contains("찾지 못했"))
                .verifyComplete();
    }
}
