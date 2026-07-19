package com.busantrip.dto.itinerary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 채팅 또는 상황 카드에서 발생한 일정 변경 요청이다. */
public record ItineraryAdjustmentRequest(
        @NotNull @Valid ItineraryRequest request,
        @NotBlank String adjustment
) {
}
