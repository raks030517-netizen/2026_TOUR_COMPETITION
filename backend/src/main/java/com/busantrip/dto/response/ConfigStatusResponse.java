package com.busantrip.dto.response;

public record ConfigStatusResponse(
        boolean naverSearchConfigured,
        boolean geminiConfigured,
        boolean weatherConfigured
) {
}

