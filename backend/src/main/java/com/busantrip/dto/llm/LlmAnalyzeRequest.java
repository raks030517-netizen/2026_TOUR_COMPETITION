package com.busantrip.dto.llm;

import jakarta.validation.constraints.NotBlank;

public record LlmAnalyzeRequest(
        @NotBlank(message = "분석할 요청을 입력해 주세요.") String message
) {
}
