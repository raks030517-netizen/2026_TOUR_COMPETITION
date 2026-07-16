package com.busantrip.dto.route;

public record RouteSummary(
        long distanceMeters,
        long durationSeconds,
        Integer fare,
        Integer transferCount,
        Integer walkingDistanceMeters,
        Integer tollFare,
        Integer taxiFare,
        Integer fuelPrice
) {
}
