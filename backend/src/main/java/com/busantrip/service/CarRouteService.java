package com.busantrip.service;

import com.busantrip.client.NaverDirectionsClient;
import com.busantrip.dto.external.route.NaverDirectionsResponse;
import com.busantrip.dto.route.RouteCoordinate;
import com.busantrip.dto.route.RouteMode;
import com.busantrip.dto.route.RoutePath;
import com.busantrip.dto.route.RouteResponse;
import com.busantrip.dto.route.RouteSegment;
import com.busantrip.dto.route.RouteSegmentType;
import com.busantrip.dto.route.RouteSummary;
import com.busantrip.exception.RouteApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CarRouteService {

    private static final String OPTION = "traoptimal";

    private final NaverDirectionsClient directionsClient;

    public CarRouteService(NaverDirectionsClient directionsClient) {
        this.directionsClient = directionsClient;
    }

    public Mono<RouteResponse> findRoute(
            String modeValue,
            Double startLongitude,
            Double startLatitude,
            Double endLongitude,
            Double endLatitude
    ) {
        RouteMode mode = parseMode(modeValue);
        if (mode != RouteMode.CAR) {
            return Mono.error(new RouteApiException(
                    "ROUTE_MODE_NOT_IMPLEMENTED",
                    "현재 단계에서는 도보 경로를 지원하지 않습니다.",
                    HttpStatus.NOT_IMPLEMENTED
            ));
        }

        validateCoordinates(startLongitude, startLatitude, endLongitude, endLatitude);
        return directionsClient.getDrivingRoute(
                        startLongitude,
                        startLatitude,
                        endLongitude,
                        endLatitude,
                        OPTION)
                .map(this::toRouteResponse);
    }

    private RouteResponse toRouteResponse(NaverDirectionsResponse response) {
        if (response == null || response.code() != 0) {
            throw providerResultException(response == null ? -1 : response.code());
        }

        List<NaverDirectionsResponse.Route> routes = response.route() == null
                ? null
                : response.route().get(OPTION);
        if (routes == null || routes.isEmpty()) {
            throw routeNotFound();
        }

        NaverDirectionsResponse.Route route = routes.getFirst();
        if (route.summary() == null || route.path() == null || route.path().isEmpty()) {
            throw routeNotFound();
        }

        List<RouteCoordinate> coordinates = route.path().stream()
                .filter(point -> point != null && point.size() >= 2
                        && point.get(0) != null && point.get(1) != null)
                .map(point -> new RouteCoordinate(point.get(0), point.get(1)))
                .toList();
        if (coordinates.size() < 2) {
            throw routeNotFound();
        }

        NaverDirectionsResponse.Summary source = route.summary();
        RouteSummary summary = new RouteSummary(
                source.distance(),
                millisecondsToSeconds(source.duration()),
                null,
                0,
                0,
                source.tollFare(),
                source.taxiFare(),
                source.fuelPrice()
        );
        List<RouteSegment> segments = route.guide() == null
                ? List.of()
                : route.guide().stream()
                        .map(guide -> new RouteSegment(
                                RouteSegmentType.DRIVE,
                                null,
                                null,
                                null,
                                millisecondsToSeconds(guide.duration()),
                                guide.distance(),
                                null,
                                guide.instructions()
                        ))
                        .toList();

        return new RouteResponse(
                RouteMode.CAR,
                summary,
                List.of(new RoutePath(RouteSegmentType.DRIVE, coordinates)),
                segments,
                List.of()
        );
    }

    private RouteMode parseMode(String value) {
        if (value == null || value.isBlank()) {
            throw invalidRequest("이동 방식을 선택해 주세요.");
        }
        try {
            return RouteMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidRequest("지원하지 않는 이동 방식입니다.");
        }
    }

    private void validateCoordinates(
            Double startLongitude,
            Double startLatitude,
            Double endLongitude,
            Double endLatitude
    ) {
        if (startLongitude == null || startLatitude == null
                || endLongitude == null || endLatitude == null) {
            throw invalidRequest("출발지와 목적지 좌표를 모두 입력해 주세요.");
        }
        if (!isLongitude(startLongitude) || !isLongitude(endLongitude)
                || !isLatitude(startLatitude) || !isLatitude(endLatitude)) {
            throw invalidRequest("위도 또는 경도 범위를 확인해 주세요.");
        }
        if (Math.abs(startLongitude - endLongitude) < 0.0000001
                && Math.abs(startLatitude - endLatitude) < 0.0000001) {
            throw new RouteApiException(
                    "SAME_LOCATION",
                    "출발지와 목적지가 같습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private boolean isLongitude(double value) {
        return Double.isFinite(value) && value >= -180 && value <= 180;
    }

    private boolean isLatitude(double value) {
        return Double.isFinite(value) && value >= -90 && value <= 90;
    }

    private long millisecondsToSeconds(long milliseconds) {
        return Math.max(0, Math.round(milliseconds / 1000.0));
    }

    private RouteApiException providerResultException(int code) {
        return switch (code) {
            case 1 -> new RouteApiException(
                    "SAME_LOCATION",
                    "출발지와 목적지가 같습니다.",
                    HttpStatus.BAD_REQUEST);
            case 2, 3, 4, 5 -> routeNotFound();
            default -> new RouteApiException(
                    "ROUTE_API_INVALID_RESPONSE",
                    "자동차 경로 응답을 처리하지 못했습니다.",
                    HttpStatus.BAD_GATEWAY);
        };
    }

    private RouteApiException routeNotFound() {
        return new RouteApiException(
                "ROUTE_NOT_FOUND",
                "선택한 출발지와 목적지 사이의 자동차 경로를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
        );
    }

    private RouteApiException invalidRequest(String message) {
        return new RouteApiException("INVALID_ROUTE_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
}
