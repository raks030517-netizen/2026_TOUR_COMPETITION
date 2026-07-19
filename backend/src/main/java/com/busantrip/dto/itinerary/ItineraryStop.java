package com.busantrip.dto.itinerary;

/** 지도와 타임라인에 함께 쓰이는 한 번의 방문 일정이다. */
public record ItineraryStop(
        String id,
        String name,
        String district,
        String category,
        String description,
        String image,
        double latitude,
        double longitude,
        String time,
        int stayMinutes,
        boolean indoor,
        String reason
) {
}
