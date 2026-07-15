package com.busantrip.service;

import com.busantrip.client.GeminiClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LlmQueryService {

    private final GeminiClient geminiClient;

    public LlmQueryService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * 사용자의 자연어 메시지에서 네이버 지역검색에 바로 쓸 수 있는 검색어를 추출한다.
     */
    public Mono<String> analyze(String message) {
        String prompt = """
                사용자의 여행 관련 메시지에서 네이버 지역검색에 사용할 검색어를 한 줄로 추출해줘.
                설명이나 따옴표 없이 검색어만 출력해.

                메시지: "%s"
                """.formatted(message);

        return geminiClient.generate(prompt).map(String::trim);
    }
}
