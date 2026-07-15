package com.busantrip.dto.response;

public record ConfigStatusResponse(
        boolean naverSearchConfigured,
        boolean gemmaConfigured,
        boolean weatherConfigured
) {
}

