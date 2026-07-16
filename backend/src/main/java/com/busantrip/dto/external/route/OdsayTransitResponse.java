package com.busantrip.dto.external.route;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayTransitResponse(
        Result result,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<Error> error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(Integer searchType, List<Path> path) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Path(
            Integer pathType,
            @JsonAlias("Info") Info info,
            List<SubPath> subPath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Info(
            Double trafficDistance,
            Integer totalWalk,
            Integer totalTime,
            Integer payment,
            Integer busTransitCount,
            Integer subwayTransitCount,
            String firstStartStation,
            String lastEndStation,
            Double totalDistance
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubPath(
            Integer trafficType,
            Double distance,
            Integer sectionTime,
            Integer stationCount,
            List<Lane> lane,
            String startName,
            Double startX,
            Double startY,
            String endName,
            Double endX,
            Double endY,
            PassStopList passStopList
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Lane(String name, String busNo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PassStopList(List<Station> stations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Station(Integer index, String stationName, Object x, Object y) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(Integer code, String msg, String message) {
        public String description() {
            return message != null ? message : msg;
        }
    }
}
