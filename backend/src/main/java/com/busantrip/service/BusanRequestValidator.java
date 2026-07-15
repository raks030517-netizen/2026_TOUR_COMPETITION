package com.busantrip.service;

import com.busantrip.dto.llm.SearchCondition;
import com.busantrip.exception.LlmResponseParseException;
import com.busantrip.exception.NonBusanRequestException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BusanRequestValidator {

    private static final Pattern OUTSIDE_BUSAN_REGION = Pattern.compile(
            "서울|인천|대구|대전|광주|울산|세종|제주|경기|강원|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도|충북|충남|전북|전남|경북|경남"
    );

    public void validateRequest(String message) {
        if (OUTSIDE_BUSAN_REGION.matcher(message).find()) {
            throw new NonBusanRequestException("부산 지역 요청만 분석할 수 있습니다.");
        }
    }

    public void validateCondition(SearchCondition condition) {
        String generatedLocations = String.join(" ",
                condition.area(), condition.tourismQuery(), condition.restaurantQuery());
        if (OUTSIDE_BUSAN_REGION.matcher(generatedLocations).find()) {
            throw new LlmResponseParseException("Gemma가 부산 외 지역 검색 조건을 반환했습니다.");
        }
    }
}
