package com.busantrip.service;

import com.busantrip.client.GemmaClient;
import com.busantrip.controller.AiChatController.Request;
import com.busantrip.controller.AiChatController.Response;
import com.busantrip.exception.GemmaConfigurationException;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GemmaChatService {

    private static final String SYSTEM_INSTRUCTION = """
            당신은 부산 여행 서비스 ROAMATE의 실시간 AI 여행 비서입니다.
            현재 위치, 선택 장소, 경로와 검색 결과를 고려해 5문장 이내의 실행 가능한
            한국어 답변을 하세요. 확인되지 않은 운영시간이나 혼잡도를 단정하지 마세요.
            """;

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "비 안 맞는 코스로 바꿔줘", "덜 걷는 일정으로 바꿔줘", "숨은 명소를 추가해줘", "지금 주변 맛집 추천해줘");
    private static final List<String> CONFIGURATION_MISSING_SUGGESTIONS = List.of(
            "현재 위치 주변 추천", "덜 걷는 일정", "실내 코스");
    private static final List<String> ERROR_SUGGESTIONS = List.of(
            "경로 다시 계산", "숨은 명소 추천", "비 피하기");

    private final GemmaClient gemmaClient;

    public GemmaChatService(GemmaClient gemmaClient) {
        this.gemmaClient = gemmaClient;
    }

    public Mono<Response> chat(Request request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return Mono.error(new IllegalArgumentException("메시지를 입력해주세요."));
        }

        String userPrompt = "문맥: " + request.context()
                + "\n최근 대화: " + request.history()
                + "\n사용자: " + request.message();

        return gemmaClient.generate(SYSTEM_INSTRUCTION, userPrompt)
                .map(text -> new Response(text, DEFAULT_SUGGESTIONS))
                .onErrorResume(GemmaConfigurationException.class, error -> Mono.just(
                        new Response("Gemma API 키가 설정되지 않았습니다.", CONFIGURATION_MISSING_SUGGESTIONS)))
                .onErrorReturn(
                        new Response("AI 응답을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", ERROR_SUGGESTIONS));
    }
}
