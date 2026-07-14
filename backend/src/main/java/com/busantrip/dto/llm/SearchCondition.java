package com.busantrip.dto.llm;

public record SearchCondition(
        SearchIntent intent,
        String area,
        String tourismQuery,
        String restaurantQuery,
        boolean trafficRequired,
        boolean busRequired,
        boolean subwayRequired
) {
}
