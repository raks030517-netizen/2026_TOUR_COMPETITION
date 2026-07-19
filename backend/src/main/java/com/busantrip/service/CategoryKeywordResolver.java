package com.busantrip.service;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 화면에서 사용하는 짧은 테마명을 관광 데이터에서 검색하기 좋은 키워드로 바꾼다.
 */
@Component
public class CategoryKeywordResolver {

    private static final Map<String, String> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("추천", "부산"),
            Map.entry("바다", "해수욕장"),
            Map.entry("시장", "시장"),
            Map.entry("카페", "카페"),
            Map.entry("문화", "문화"),
            Map.entry("체험", "체험"),
            Map.entry("공원", "공원"),
            Map.entry("야경", "야경")
    );

    public String resolve(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        String normalizedInput = input.trim();
        return CATEGORY_KEYWORDS.getOrDefault(normalizedInput, normalizedInput);
    }
}
