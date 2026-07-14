package com.roamate.backend.domain.schedule.dto;

import com.roamate.backend.domain.schedule.Schedule;
import com.roamate.backend.domain.schedule.ScheduleStatus;
import java.time.LocalDate;
import java.util.List;

public record ScheduleResponse(
        Long id,
        String title,
        LocalDate travelDate,
        ScheduleStatus status,
        List<ScheduleItemResponse> items,
        ScheduleAiStatusResponse aiStatus
) {
    public static ScheduleResponse of(Schedule schedule, List<ScheduleItemResponse> items, ScheduleAiStatusResponse aiStatus) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getTravelDate(),
                schedule.getStatus(),
                items,
                aiStatus
        );
    }
}
