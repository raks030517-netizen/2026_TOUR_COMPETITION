package com.busantrip.service;

import com.busantrip.client.GemmaClient;
import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.exception.LlmAnalysisFailedException;
import com.busantrip.exception.LlmResponseParseException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LlmQueryService {

    static final String SYSTEM_INSTRUCTION = """
            당신은 부산 관광 검색 조건 분석기입니다.

            사용자의 요청을 네이버 지역 검색 API에서 사용할 검색 조건으로 변환하세요.

            규칙:
            1. 부산 지역만 처리합니다.
            2. 장소를 직접 추천하지 않습니다.
            3. 존재하지 않는 장소명이나 주소를 만들지 않습니다.
            4. 관광지 검색어와 음식점 검색어를 생성합니다.
            5. 교통량이 필요하면 trafficRequired를 true로 설정합니다.
            6. 버스나 정류장을 언급하면 busRequired를 true로 설정합니다.
            7. 지하철이나 도시철도를 언급하면 subwayRequired를 true로 설정합니다.
            8. 설명이나 Markdown 없이 JSON 객체만 반환합니다.
            9. 알 수 없는 값은 빈 문자열 또는 false로 반환합니다.

            intent는 관광지만 찾으면 TOURISM_SEARCH, 음식점만 찾으면 RESTAURANT_SEARCH,
            관광지와 음식점을 함께 찾으면 COURSE_SEARCH로 설정합니다.
            area와 검색어에는 부산을 명시합니다.
            다음 필드를 모두 포함한 JSON 객체 하나만 반환합니다:
            intent, area, tourismQuery, restaurantQuery,
            trafficRequired, busRequired, subwayRequired.
            """;

    private final GemmaClient gemmaClient;
    private final SearchConditionParser parser;
    private final BusanRequestValidator validator;

    public LlmQueryService(
            GemmaClient gemmaClient,
            SearchConditionParser parser,
            BusanRequestValidator validator
    ) {
        this.gemmaClient = gemmaClient;
        this.parser = parser;
        this.validator = validator;
    }

    public Mono<SearchCondition> analyze(String message) {
        String normalizedMessage = message.trim();
        validator.validateRequest(normalizedMessage);

        return analyzeOnce(normalizedMessage)
                .onErrorResume(LlmResponseParseException.class,
                        error -> analyzeOnce(correctionPrompt(normalizedMessage)))
                .onErrorMap(LlmResponseParseException.class,
                        error -> new LlmAnalysisFailedException(
                                "Gemma 응답을 두 번 모두 구조화하지 못했습니다.", error));
    }

    private Mono<SearchCondition> analyzeOnce(String userPrompt) {
        return gemmaClient.generate(SYSTEM_INSTRUCTION, userPrompt)
                .map(parser::parse)
                .doOnNext(validator::validateCondition);
    }

    private String correctionPrompt(String originalMessage) {
        return """
                이전 응답의 JSON 형식 또는 필드 값이 올바르지 않았습니다.
                아래 사용자 요청을 다시 분석하고, 지정된 필드를 모두 포함한 JSON 객체 하나만 반환하세요.

                사용자 요청: %s
                """.formatted(originalMessage);
    }
}
