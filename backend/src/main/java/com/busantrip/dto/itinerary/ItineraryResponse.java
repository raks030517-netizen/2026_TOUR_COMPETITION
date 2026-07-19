package com.busantrip.dto.itinerary;

import com.busantrip.controller.RouteController.RouteResponse;
import java.util.List;

/** 일정 생성 결과. route는 기존 지도 컴포넌트의 경로 계약을 그대로 재사용한다. */
public record ItineraryResponse(
        String title,
        String summary,
        String source,
        TravelContext context,
        List<ItineraryDay> days,
        List<ItineraryStop> orderedPlaces,
        RouteResponse route,
        List<String> tips
) {
}
