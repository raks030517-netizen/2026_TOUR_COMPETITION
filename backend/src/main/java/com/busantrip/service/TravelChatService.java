package com.busantrip.service;

import com.busantrip.dto.request.ChatRequest;
import com.busantrip.dto.response.ChatResponse;
import com.busantrip.dto.response.PlaceResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TravelChatService {

    private final LlmQueryService llmQueryService;
    private final NaverLocalSearchService naverLocalSearchService;

    public TravelChatService(LlmQueryService llmQueryService, NaverLocalSearchService naverLocalSearchService) {
        this.llmQueryService = llmQueryService;
        this.naverLocalSearchService = naverLocalSearchService;
    }

    /**
     * 자연어 메시지 → 검색어 추출(Gemini) → 장소 검색(네이버) → 응답 조립 순으로 처리한다.
     * 검색결과를 다시 Gemini에 넣어 대화체 답장을 만드는 건 v2로 미룸(API 호출 왕복 2배 방지).
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        return llmQueryService.analyze(request.message())
                .flatMap(naverLocalSearchService::search)
                .map(places -> new ChatResponse(buildReplyMessage(places), places));
    }

    private String buildReplyMessage(List<PlaceResponse> places) {
        if (places.isEmpty()) {
            return "조건에 맞는 장소를 찾지 못했어요. 다른 키워드로 다시 물어봐 주세요.";
        }
        return "이런 곳은 어떠세요? " + places.size() + "곳을 찾았어요.";
    }
}
