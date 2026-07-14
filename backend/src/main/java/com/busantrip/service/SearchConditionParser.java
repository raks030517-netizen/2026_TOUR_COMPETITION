package com.busantrip.service;

import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.dto.llm.SearchIntent;
import com.busantrip.exception.LlmResponseParseException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class SearchConditionParser {

    private final ObjectMapper objectMapper;

    public SearchConditionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SearchCondition parse(String modelOutput) {
        try {
            JsonNode root = objectMapper.readTree(extractFirstJsonObject(modelOutput));
            if (root == null || !root.isObject()) {
                throw new LlmResponseParseException("Gemma 응답이 JSON 객체가 아닙니다.");
            }

            String tourismQuery = text(root, "tourismQuery");
            String restaurantQuery = text(root, "restaurantQuery");
            SearchIntent intent = parseIntent(text(root, "intent"), tourismQuery, restaurantQuery);

            return new SearchCondition(
                    intent,
                    text(root, "area"),
                    tourismQuery,
                    restaurantQuery,
                    bool(root, "trafficRequired"),
                    bool(root, "busRequired"),
                    bool(root, "subwayRequired")
            );
        } catch (LlmResponseParseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LlmResponseParseException("Gemma 응답 JSON을 파싱하지 못했습니다.", exception);
        }
    }

    private SearchIntent parseIntent(String value, String tourismQuery, String restaurantQuery) {
        if (value.isBlank()) {
            if (!tourismQuery.isBlank() && !restaurantQuery.isBlank()) {
                return SearchIntent.COURSE_SEARCH;
            }
            if (!tourismQuery.isBlank()) {
                return SearchIntent.TOURISM_SEARCH;
            }
            if (!restaurantQuery.isBlank()) {
                return SearchIntent.RESTAURANT_SEARCH;
            }
            throw new LlmResponseParseException("Gemma 응답에서 검색 의도를 확인할 수 없습니다.");
        }

        try {
            return SearchIntent.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new LlmResponseParseException("Gemma 응답의 검색 의도가 올바르지 않습니다.", exception);
        }
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        return value != null && value.isString() ? value.stringValue().trim() : "";
    }

    private boolean bool(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        return value != null && value.isBoolean() && value.booleanValue();
    }

    private String extractFirstJsonObject(String output) {
        if (output == null || output.isBlank()) {
            throw new LlmResponseParseException("Gemma 응답이 비어 있습니다.");
        }

        int start = output.indexOf('{');
        if (start < 0) {
            throw new LlmResponseParseException("Gemma 응답에서 JSON 객체를 찾지 못했습니다.");
        }

        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;
        for (int index = start; index < output.length(); index++) {
            char current = output.charAt(index);
            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    insideString = false;
                }
                continue;
            }

            if (current == '"') {
                insideString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return output.substring(start, index + 1);
            }
        }

        throw new LlmResponseParseException("Gemma 응답의 JSON 객체가 완전하지 않습니다.");
    }
}
