package com.roamate.backend.domain.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public record ScheduleAiStatusUpdateRequest(
        @Min(0) @Max(100) Integer successProbability,
        String statusEmoji,
        String statusText,
        String nextPlaceName,
        LocalDateTime estimatedArrivalAt,
        Integer travelMinutes,
        String recommendedAction,
        String riskReasons
) {
}
