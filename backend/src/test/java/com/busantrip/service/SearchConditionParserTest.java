package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.dto.llm.SearchIntent;
import com.busantrip.exception.LlmResponseParseException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SearchConditionParserTest {

    private final SearchConditionParser parser = new SearchConditionParser(new ObjectMapper());

    @Test
    void parsesNormalJson() {
        SearchCondition result = parser.parse("""
                {
                  "intent": "COURSE_SEARCH",
                  "area": "부산 광안리",
                  "tourismQuery": "부산 광안리 관광지",
                  "restaurantQuery": "부산 광안리 조개구이",
                  "trafficRequired": false,
                  "busRequired": false,
                  "subwayRequired": false
                }
                """);

        assertThat(result).isEqualTo(new SearchCondition(
                SearchIntent.COURSE_SEARCH,
                "부산 광안리",
                "부산 광안리 관광지",
                "부산 광안리 조개구이",
                false,
                false,
                false
        ));
    }

    @Test
    void extractsJsonFromExplanationAndMarkdownCodeBlock() {
        SearchCondition result = parser.parse("""
                분석 결과입니다.
                ```json
                {
                  "intent": "TOURISM_SEARCH",
                  "area": "부산 해운대",
                  "tourismQuery": "부산 해운대 관광지"
                }
                ```
                위 조건을 사용하세요.
                """);

        assertThat(result.intent()).isEqualTo(SearchIntent.TOURISM_SEARCH);
        assertThat(result.area()).isEqualTo("부산 해운대");
        assertThat(result.tourismQuery()).isEqualTo("부산 해운대 관광지");
    }

    @Test
    void suppliesSafeDefaultsForMissingFieldsAndInfersIntent() {
        SearchCondition result = parser.parse("""
                {"restaurantQuery":"부산 서면 돼지국밥"}
                """);

        assertThat(result).isEqualTo(new SearchCondition(
                SearchIntent.RESTAURANT_SEARCH,
                "",
                "",
                "부산 서면 돼지국밥",
                false,
                false,
                false
        ));
    }

    @Test
    void rejectsUnknownIntent() {
        assertThatThrownBy(() -> parser.parse("""
                {"intent":"UNKNOWN_SEARCH","tourismQuery":"부산 관광지"}
                """))
                .isInstanceOf(LlmResponseParseException.class);
    }
}
