package com.busantrip.service;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

import com.busantrip.client.GeminiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LlmQueryServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Test
    void 사용자_메시지를_프롬프트에_담아_Gemini를_호출하고_응답의_앞뒤_공백을_정리한다() {
        String userMessage = "부산에서 조용한 카페 추천해줘";
        when(geminiClient.generate(contains(userMessage)))
                .thenReturn(Mono.just("  부산 조용한 카페  \n"));

        LlmQueryService service = new LlmQueryService(geminiClient);

        StepVerifier.create(service.analyze(userMessage))
                .expectNext("부산 조용한 카페")
                .verifyComplete();
    }
}
