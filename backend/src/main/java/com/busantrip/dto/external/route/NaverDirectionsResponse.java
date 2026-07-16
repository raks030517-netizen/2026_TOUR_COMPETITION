package com.busantrip.dto.external.route;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverDirectionsResponse(
        int code,
        String message,
        Map<String, List<Route>> route
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            Summary summary,
            List<List<Double>> path,
            List<Guide> guide
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            long distance,
            long duration,
            Integer tollFare,
            Integer taxiFare,
            Integer fuelPrice
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guide(
            int pointIndex,
            String instructions,
            long distance,
            long duration
    ) {
    }
}
