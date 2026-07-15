package com.busantrip.dto.travel;

import jakarta.validation.constraints.NotBlank;

public record TravelSearchRequest(
        @NotBlank(message = "여행 검색 요청을 입력해 주세요.") String message
) {
}
