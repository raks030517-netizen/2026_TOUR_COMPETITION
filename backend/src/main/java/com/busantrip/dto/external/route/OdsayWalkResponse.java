package com.busantrip.dto.external.route;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayWalkResponse(
        Result result,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<Error> error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            List<PathResult> path
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PathResult(
            Boolean hasPathResult,
            String errorCode,
            Double speedKMPerHour,
            WalkPath recommend
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WalkPath(
            Summary summary,
            List<Route> routes,
            List<Guide> guides
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(Point start, Point end, Integer distance, Integer duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Point(Double x, Double y) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            Integer type,
            Integer facility,
            Integer distance,
            Integer duration,
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            List<Point> coordinate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guide(
            Integer routeIndex,
            Integer angle,
            Integer turn,
            Integer facility,
            String guidance,
            Point point
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(Integer code, String msg, String message) {
        public String description() {
            return message != null ? message : msg;
        }
    }
}
