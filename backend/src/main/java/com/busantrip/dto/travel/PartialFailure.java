package com.busantrip.dto.travel;

public record PartialFailure(
        FailureProvider provider,
        String message
) {
}
