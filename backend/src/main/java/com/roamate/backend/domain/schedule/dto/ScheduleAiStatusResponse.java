package com.roamate.backend.domain.schedule.dto;

import com.roamate.backend.domain.schedule.ScheduleAiStatus;
import java.time.LocalDateTime;

public record ScheduleAiStatusResponse(
        Integer successProbability,
        String statusEmoji,
        String statusText,
        String nextPlaceName,
        LocalDateTime estimatedArrivalAt,
        Integer travelMinutes,
        String recommendedAction,
        String riskReasons
) {
    public static ScheduleAiStatusResponse from(ScheduleAiStatus status) {
        return new ScheduleAiStatusResponse(
                status.getSuccessProbability(),
                status.getStatusEmoji(),
                status.getStatusText(),
                status.getNextPlaceName(),
                status.getEstimatedArrivalAt(),
                status.getTravelMinutes(),
                status.getRecommendedAction(),
                status.getRiskReasons()
        );
    }
}
