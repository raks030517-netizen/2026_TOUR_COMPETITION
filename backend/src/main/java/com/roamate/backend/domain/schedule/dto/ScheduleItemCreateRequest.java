package com.roamate.backend.domain.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleItemCreateRequest(
        @NotNull Long placeId,
        @Min(0) int visitOrder,
        LocalDateTime plannedArrival,
        LocalDateTime plannedDeparture
) {
}
