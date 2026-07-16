package com.busantrip.service;

import com.busantrip.client.OdsayTransitClient;
import com.busantrip.dto.external.route.OdsayTransitResponse;
import com.busantrip.dto.route.RouteCoordinate;
import com.busantrip.dto.route.RouteMode;
import com.busantrip.dto.route.RoutePath;
import com.busantrip.dto.route.RouteResponse;
import com.busantrip.dto.route.RouteSegment;
import com.busantrip.dto.route.RouteSegmentType;
import com.busantrip.dto.route.RouteSummary;
import com.busantrip.exception.RouteApiException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TransitRouteService {

    private final OdsayTransitClient transitClient;

    public TransitRouteService(OdsayTransitClient transitClient) {
        this.transitClient = transitClient;
    }

    public Mono<List<RouteResponse>> findRoutes(
            Double startLongitude,
            Double startLatitude,
            Double endLongitude,
            Double endLatitude
    ) {
        validateCoordinates(startLongitude, startLatitude, endLongitude, endLatitude);
        return transitClient.findRoutes(
                        startLongitude, startLatitude, endLongitude, endLatitude)
                .map(this::toRouteResponses);
    }

    private List<RouteResponse> toRouteResponses(OdsayTransitResponse response) {
        if (response == null) {
            throw invalidResponse();
        }
        if (response.error() != null && !response.error().isEmpty()) {
            throw providerException(response.error().getFirst());
        }
        if (response.result() == null || response.result().path() == null
                || response.result().path().isEmpty()) {
            throw routeNotFound();
        }
        if (response.result().searchType() != null && response.result().searchType() != 0) {
            throw new RouteApiException(
                    "TRANSIT_INTERCITY_UNSUPPORTED",
                    "현재는 부산 시내 대중교통 경로만 지원합니다.",
                    HttpStatus.NOT_IMPLEMENTED);
        }

        List<RouteResponse> routes = response.result().path().stream()
                .filter(Objects::nonNull)
                .map(this::toRouteResponse)
                .toList();
        if (routes.isEmpty()) {
            throw routeNotFound();
        }
        return routes;
    }

    private RouteResponse toRouteResponse(OdsayTransitResponse.Path source) {
        if (source.info() == null || source.subPath() == null) {
            throw invalidResponse();
        }

        List<RouteSegment> segments = source.subPath().stream()
                .filter(Objects::nonNull)
                .map(this::toSegment)
                .toList();
        List<RoutePath> paths = source.subPath().stream()
                .filter(Objects::nonNull)
                .map(this::toPath)
                .filter(Objects::nonNull)
                .toList();
        int transportSegmentCount = (int) segments.stream()
                .filter(segment -> segment.type() != RouteSegmentType.WALK)
                .count();
        OdsayTransitResponse.Info info = source.info();
        RouteSummary summary = new RouteSummary(
                rounded(info.totalDistance()),
                minutesToSeconds(info.totalTime()),
                info.payment(),
                Math.max(0, transportSegmentCount - 1),
                info.totalWalk(),
                null,
                null,
                null);
        List<String> warnings = paths.isEmpty()
                ? List.of("ODsay 응답에 지도에 표시할 실제 경로 좌표가 없습니다.")
                : List.of();

        return new RouteResponse(RouteMode.TRANSIT, summary, paths, segments, warnings);
    }

    private RouteSegment toSegment(OdsayTransitResponse.SubPath source) {
        RouteSegmentType type = segmentType(source.trafficType());
        return new RouteSegment(
                type,
                transportName(source.lane()),
                source.startName(),
                source.endName(),
                minutesToSecondsNullable(source.sectionTime()),
                roundedNullable(source.distance()),
                source.stationCount(),
                null);
    }

    private RoutePath toPath(OdsayTransitResponse.SubPath source) {
        RouteSegmentType type = segmentType(source.trafficType());
        if (type == RouteSegmentType.WALK || source.passStopList() == null
                || source.passStopList().stations() == null) {
            return null;
        }
        List<RouteCoordinate> coordinates = source.passStopList().stations().stream()
                .map(station -> coordinate(station.x(), station.y()))
                .filter(Objects::nonNull)
                .toList();
        return coordinates.size() >= 2 ? new RoutePath(type, coordinates) : null;
    }

    private RouteCoordinate coordinate(Object x, Object y) {
        try {
            double longitude = Double.parseDouble(String.valueOf(x));
            double latitude = Double.parseDouble(String.valueOf(y));
            if (isLongitude(longitude) && isLatitude(latitude)) {
                return new RouteCoordinate(longitude, latitude);
            }
        } catch (NumberFormatException ignored) {
            // 제공자가 좌표를 생략하거나 숫자가 아닌 값을 반환하면 지도 경로에서 제외한다.
        }
        return null;
    }

    private RouteSegmentType segmentType(Integer trafficType) {
        if (trafficType == null) {
            throw invalidResponse();
        }
        return switch (trafficType) {
            case 1 -> RouteSegmentType.SUBWAY;
            case 2 -> RouteSegmentType.BUS;
            case 3 -> RouteSegmentType.WALK;
            default -> throw invalidResponse();
        };
    }

    private String transportName(List<OdsayTransitResponse.Lane> lanes) {
        if (lanes == null) {
            return null;
        }
        String joined = lanes.stream()
                .map(lane -> lane.busNo() != null ? lane.busNo() : lane.name())
                .filter(Objects::nonNull)
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
        return joined == null || joined.isBlank() ? null : joined;
    }

    private void validateCoordinates(Double startLng, Double startLat, Double endLng, Double endLat) {
        if (startLng == null || startLat == null || endLng == null || endLat == null) {
            throw invalidRequest("출발지와 목적지 좌표를 모두 입력해 주세요.");
        }
        if (!isLongitude(startLng) || !isLongitude(endLng)
                || !isLatitude(startLat) || !isLatitude(endLat)) {
            throw invalidRequest("위도 또는 경도 범위를 확인해 주세요.");
        }
        if (Math.abs(startLng - endLng) < 0.0000001
                && Math.abs(startLat - endLat) < 0.0000001) {
            throw new RouteApiException("SAME_LOCATION", "출발지와 목적지가 같습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private RouteApiException providerException(OdsayTransitResponse.Error error) {
        String description = error.description() == null ? "" : error.description();
        if (description.contains("ApiKeyAuthFailed")) {
            return new RouteApiException(
                    "ODSAY_AUTHENTICATION_FAILED",
                    "ODsay API Key와 등록된 서버 IP를 확인해 주세요.",
                    HttpStatus.BAD_GATEWAY);
        }
        if (error.code() != null && List.of(3, 4, 5, 6, -98, -99).contains(error.code())) {
            return routeNotFound();
        }
        return invalidResponse();
    }

    private long rounded(Double value) {
        return value == null ? 0 : Math.max(0, Math.round(value));
    }

    private Long roundedNullable(Double value) {
        return value == null ? null : Math.max(0, Math.round(value));
    }

    private long minutesToSeconds(Integer minutes) {
        return minutes == null ? 0 : Math.max(0, minutes.longValue() * 60);
    }

    private Long minutesToSecondsNullable(Integer minutes) {
        return minutes == null ? null : Math.max(0, minutes.longValue() * 60);
    }

    private boolean isLongitude(double value) {
        return Double.isFinite(value) && value >= -180 && value <= 180;
    }

    private boolean isLatitude(double value) {
        return Double.isFinite(value) && value >= -90 && value <= 90;
    }

    private RouteApiException routeNotFound() {
        return new RouteApiException(
                "TRANSIT_ROUTE_NOT_FOUND",
                "선택한 출발지와 목적지 사이의 대중교통 경로를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND);
    }

    private RouteApiException invalidResponse() {
        return new RouteApiException(
                "TRANSIT_API_INVALID_RESPONSE",
                "대중교통 경로 응답을 처리하지 못했습니다.",
                HttpStatus.BAD_GATEWAY);
    }

    private RouteApiException invalidRequest(String message) {
        return new RouteApiException("INVALID_ROUTE_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
}
