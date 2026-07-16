package com.busantrip.service;

import com.busantrip.dto.ai.AiTravelBrief;
import com.busantrip.dto.llm.SearchCondition;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class SpringAiMappingService {

    private final ChatClient chatClient;
    private final BusanRequestValidator validator;

    public SpringAiMappingService(ChatClient.Builder builder, BusanRequestValidator validator) {
        this.chatClient = builder.build();
        this.validator = validator;
    }

    public Mono<SearchCondition> mapSearchCondition(String message) {
        validator.validateRequest(message);
        return Mono.fromCallable(() -> chatClient.prompt()
                        .system("""
                                부산 관광 검색 요청을 구조화한다. 실제 장소를 만들어내지 말고 검색어만 작성한다.
                                intent는 TOURISM_SEARCH, RESTAURANT_SEARCH, COURSE_SEARCH 중 하나다.
                                area와 검색어에는 부산을 포함하고 요청에 따라 교통, 버스, 지하철 필요 여부를 표시한다.
                                """)
                        .user(message.trim())
                        .call()
                        .entity(SearchCondition.class, spec -> spec.useProviderStructuredOutput()))
                .doOnNext(validator::validateCondition)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AiTravelBrief> mapTravelBrief(String message) {
        validator.validateRequest(message);
        return Mono.fromCallable(() -> chatClient.prompt()
                        .system("""
                                부산 여행 요청을 짧은 데모 브리프로 구조화한다.
                                확인되지 않은 영업시간, 가격, 날씨를 사실처럼 단정하지 않는다.
                                title, summary, recommendedArea, activities, cautions를 한국어로 작성한다.
                                """)
                        .user(message.trim())
                        .call()
                        .entity(AiTravelBrief.class, spec -> spec.useProviderStructuredOutput()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
