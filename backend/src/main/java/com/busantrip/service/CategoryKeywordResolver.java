package com.busantrip.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 화면에서 전달받은 관광 카테고리를
 * 관광공사 API 검색에 적합한 키워드로 변환한다.
 */
@Component
public class CategoryKeywordResolver {

    private static final Map<String, String> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("추천", "해운대"),
            Map.entry("바다", "해수욕장"),
            Map.entry("시장", "시장"),
            Map.entry("카페", "카페"),
            Map.entry("문화", "박물관"),
            Map.entry("체험", "체험"),
            Map.entry("공원", "공원"),
            Map.entry("야경", "전망대")
    );

    /**
     * 등록된 카테고리라면 실제 검색어로 변환하고,
     * 일반 검색어라면 원문을 그대로 반환한다.
     *
     * 예:
     * 바다 → 해수욕장
     * 해운대 → 해운대
     */
    public String resolve(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "검색어는 비어 있을 수 없습니다."
            );
        }

        String normalizedInput = input.trim();

        return CATEGORY_KEYWORDS.getOrDefault(
                normalizedInput,
                normalizedInput
        );
    }
}