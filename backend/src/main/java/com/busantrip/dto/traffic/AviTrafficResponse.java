package com.busantrip.dto.traffic;

public record AviTrafficResponse(
        String stationName,
        String measuredAt,
        long trafficVolume,
        double latitude,
        double longitude
) {
}
