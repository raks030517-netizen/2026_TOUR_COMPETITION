package com.busantrip.dto.itinerary;

import java.util.List;

/** 외부 데이터와 데모 폴백을 같은 계약으로 전달하는 여행 상황 요약이다. */
public record TravelContext(
        Signal weather,
        Signal traffic,
        List<String> alerts
) {
    public record Signal(String label, String detail, boolean live) {
    }
}
