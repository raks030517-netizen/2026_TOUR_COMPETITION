package com.roamate.backend.domain.schedule.dto;

import java.time.LocalDateTime;

public record ScheduleItemUpdateRequest(
        LocalDateTime plannedArrival,
        LocalDateTime plannedDeparture
) {
}
