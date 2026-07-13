package com.busantrip.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "검색 문장을 입력해 주세요.") String message
) {
}

