package com.roamate.backend.domain.place.dto;

import com.roamate.backend.domain.place.condition.CongestionLevel;
import com.roamate.backend.domain.place.condition.PlaceCondition;
import java.time.LocalDateTime;

public record PlaceConditionResponse(
        CongestionLevel congestionLevel,
        String weatherAlert,
        Integer trafficDelayMinutes,
        LocalDateTime observedAt
) {
    public static PlaceConditionResponse from(PlaceCondition condition) {
        return new PlaceConditionResponse(
                condition.congestionLevel(),
                condition.weatherAlert(),
                condition.trafficDelayMinutes(),
                condition.observedAt()
        );
    }
}
