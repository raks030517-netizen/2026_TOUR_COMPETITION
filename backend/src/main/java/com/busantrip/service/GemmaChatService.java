package com.busantrip.service;

import com.busantrip.client.GemmaClient;
import com.busantrip.controller.AiChatController.Request;
import com.busantrip.controller.AiChatController.Response;
import com.busantrip.exception.GemmaConfigurationException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GemmaChatService {

    private static final String SYSTEM_INSTRUCTION = """
            당신은 부산 여행 서비스 ROAMATE의 실시간 AI 여행 메이트입니다.
            사용자의 현재 위치, 선택한 장소, 경로와 대화 문맥을 고려해 5문장 이내의
            친절하고 실행 가능한 한국어 답변을 제공합니다. 확인하지 못한 영업시간,
            날씨, 교통상황을 사실처럼 단정하지 마세요.
            """;

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "비가 오면 실내 코스로 바꿔줘",
            "걷는 시간을 줄여줘",
            "지금 근처 카페를 추천해줘",
            "야경 명소를 추가해줘"
    );

    private final GemmaClient gemmaClient;

    public GemmaChatService(GemmaClient gemmaClient) {
        this.gemmaClient = gemmaClient;
    }

    public Mono<Response> chat(Request request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return Mono.error(new IllegalArgumentException("메시지를 입력해 주세요."));
        }

        String userPrompt = "여행 상태: " + request.context()
                + "\n최근 대화: " + request.history()
                + "\n사용자 요청: " + request.message();

        // GemmaClient validates configuration before returning its Mono. Defer keeps
        // that synchronous validation error inside the reactive chain so demo mode
        // can handle it in exactly the same way as a network/API failure.
        return Mono.defer(() -> gemmaClient.generate(SYSTEM_INSTRUCTION, userPrompt))
                .map(text -> new Response(text, DEFAULT_SUGGESTIONS))
                .onErrorResume(GemmaConfigurationException.class, error -> Mono.just(demoResponse(request)))
                .onErrorResume(error -> Mono.just(demoResponse(request)));
    }

    private Response demoResponse(Request request) {
        String message = request.message().toLowerCase(Locale.ROOT);
        if (containsAny(message, "비", "비가", "실내")) {
            return new Response(
                    "비가 올 때는 야외 이동을 줄이는 편이 좋아요. 감천문화마을이나 F1963처럼 머무를 공간이 있는 장소를 먼저 두고, 이동 전에는 운영 여부를 한 번 확인해 주세요.",
                    List.of("실내 중심으로 다시 짜줘", "카페를 추가해줘", "걷는 시간을 줄여줘")
            );
        }
        if (containsAny(message, "카페", "커피", "휴식")) {
            return new Response(
                    "일정 사이에 40분 정도의 휴식 시간을 넣어볼게요. 선택한 장소와 같은 구의 카페를 다음 목적지로 두면 이동 부담을 줄일 수 있어요.",
                    List.of("조용한 카페 위주로", "디저트 카페를 찾아줘", "다음 장소로 이동")
            );
        }
        if (containsAny(message, "걷", "피곤", "택시", "줄여")) {
            return new Response(
                    "걷는 구간을 줄이도록 가까운 장소부터 방문하는 순서를 권해요. 현재 경로를 다시 계산하면 출발지 기준으로 더 짧은 동선을 확인할 수 있습니다.",
                    List.of("경로 다시 계산", "가까운 곳만 남겨줘", "휴식 장소 추가")
            );
        }
        if (containsAny(message, "야경", "밤", "노을")) {
            return new Response(
                    "마지막 일정에 광안리, 용두산공원, 다대포처럼 전망을 즐길 수 있는 장소를 두면 좋아요. 해가 지는 시간과 귀가 교통편은 출발 전에 확인해 주세요.",
                    List.of("야경 코스로 다시 짜줘", "광안리 주변을 추천해줘", "이동 시간을 줄여줘")
            );
        }

        return new Response(
                "좋아요. 지금 선택한 장소와 현재 위치를 기준으로 여행을 이어가 볼게요. 원하시는 테마나 이동 방식, 쉬고 싶은 시간을 알려주시면 다음 일정을 더 구체적으로 조정해 드릴 수 있어요.",
                DEFAULT_SUGGESTIONS
        );
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
