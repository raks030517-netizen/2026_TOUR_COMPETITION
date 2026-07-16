package com.busantrip.dto.route;

public record RouteSegment(
        RouteSegmentType type,
        String transportName,
        String startName,
        String endName,
        Long durationSeconds,
        Long distanceMeters,
        Integer stationCount,
        String instruction
) {
}
