package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.busantrip.client.GemmaClient;
import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.dto.llm.SearchIntent;
import com.busantrip.exception.LlmAnalysisFailedException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

class LlmQueryServiceTest {

    @Test
    void retriesOnlyOnceWhenFirstResponseCannotBeParsed() {
        GemmaClient client = mock(GemmaClient.class);
        when(client.generate(anyString(), anyString()))
                .thenReturn(Mono.just("JSON이 아닌 응답"))
                .thenReturn(Mono.just("""
                        {
                          "intent":"RESTAURANT_SEARCH",
                          "area":"부산 광안리",
                          "restaurantQuery":"부산 광안리 조개구이"
                        }
                        """)
                );
        LlmQueryService service = new LlmQueryService(
                client,
                new SearchConditionParser(new ObjectMapper()),
                new BusanRequestValidator()
        );

        SearchCondition result = service.analyze("광안리에서 조개구이를 먹고 싶어요.").block();

        assertThat(result).isEqualTo(new SearchCondition(
                SearchIntent.RESTAURANT_SEARCH,
                "부산 광안리",
                "",
                "부산 광안리 조개구이",
                false,
                false,
                false
        ));
        verify(client, times(2)).generate(anyString(), anyString());
    }

    @Test
    void returnsAnalysisFailureAfterOneCorrectionAlsoFails() {
        GemmaClient client = mock(GemmaClient.class);
        when(client.generate(anyString(), anyString()))
                .thenReturn(Mono.just("첫 번째 잘못된 응답"))
                .thenReturn(Mono.just("두 번째 잘못된 응답"));
        LlmQueryService service = new LlmQueryService(
                client,
                new SearchConditionParser(new ObjectMapper()),
                new BusanRequestValidator()
        );

        assertThatThrownBy(() -> service.analyze("해운대 관광지를 알려주세요.").block())
                .isInstanceOf(LlmAnalysisFailedException.class);
        verify(client, times(2)).generate(anyString(), anyString());
    }
}
