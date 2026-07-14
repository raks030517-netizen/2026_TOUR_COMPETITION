package com.roamate.backend.domain.schedule.dto;

import com.roamate.backend.domain.schedule.ScheduleItem;
import java.time.LocalDateTime;

public record ScheduleItemResponse(
        Long id,
        Long placeId,
        String placeName,
        int visitOrder,
        LocalDateTime plannedArrival,
        LocalDateTime plannedDeparture
) {
    public static ScheduleItemResponse from(ScheduleItem item) {
        return new ScheduleItemResponse(
                item.getId(),
                item.getPlace().getId(),
                item.getPlace().getName(),
                item.getVisitOrder(),
                item.getPlannedArrival(),
                item.getPlannedDeparture()
        );
    }
}
