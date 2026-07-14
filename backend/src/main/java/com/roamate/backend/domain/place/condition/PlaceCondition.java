package com.roamate.backend.domain.place.condition;

import java.time.LocalDateTime;

/**
 * 관광공사 혼잡도·기상특보·교통 API(BE-1)를 하나로 합친 장소의 "지금 상황" 스냅샷.
 * BE-1이 실제 구현체(PlaceConditionProvider)를 붙이기 전까지는 UNKNOWN으로 채워진 값이 온다.
 */
public record PlaceCondition(
        CongestionLevel congestionLevel,
        String weatherAlert,
        Integer trafficDelayMinutes,
        LocalDateTime observedAt
) {
    public static PlaceCondition unknown(LocalDateTime observedAt) {
        return new PlaceCondition(CongestionLevel.UNKNOWN, null, null, observedAt);
    }
}
