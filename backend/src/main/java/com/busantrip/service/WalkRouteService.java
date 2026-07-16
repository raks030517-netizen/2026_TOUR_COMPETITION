package com.busantrip.service;

import com.busantrip.client.OdsayWalkClient;
import com.busantrip.dto.external.route.OdsayWalkResponse;
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
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WalkRouteService {

    private final OdsayWalkClient walkClient;

    public WalkRouteService(OdsayWalkClient walkClient) {
        this.walkClient = walkClient;
    }

    public Mono<RouteResponse> findRoute(
            Double startLongitude,
            Double startLatitude,
            Double endLongitude,
            Double endLatitude
    ) {
        validateCoordinates(startLongitude, startLatitude, endLongitude, endLatitude);
        return walkClient.findRoute(startLongitude, startLatitude, endLongitude, endLatitude)
                .map(this::toRouteResponse);
    }

    private RouteResponse toRouteResponse(OdsayWalkResponse response) {
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

        OdsayWalkResponse.PathResult result = response.result().path().getFirst();
        if (!Boolean.TRUE.equals(result.hasPathResult())) {
            throw pathError(result.errorCode());
        }
        if (result.recommend() == null || result.recommend().summary() == null) {
            throw invalidResponse();
        }

        OdsayWalkResponse.WalkPath source = result.recommend();
        OdsayWalkResponse.Summary sourceSummary = source.summary();
        int distance = nonNegative(sourceSummary.distance());
        int duration = nonNegative(sourceSummary.duration());
        List<OdsayWalkResponse.Route> sourceRoutes = source.routes() == null
                ? List.of() : source.routes();
        List<RoutePath> paths = sourceRoutes.stream()
                .map(this::toPath)
                .filter(Objects::nonNull)
                .toList();
        List<RouteSegment> segments = IntStream.range(0, sourceRoutes.size())
                .mapToObj(index -> toSegment(sourceRoutes.get(index), source.guides(), index))
                .toList();
        List<String> warnings = paths.isEmpty()
                ? List.of("ODsay 응답에 실제 도보 경로 좌표가 없어 거리와 시간만 표시합니다.")
                : List.of();

        return new RouteResponse(
                RouteMode.WALK,
                new RouteSummary(distance, duration, null, 0, distance, null, null, null),
                paths,
                segments,
                warnings);
    }

    private RoutePath toPath(OdsayWalkResponse.Route source) {
        if (source.coordinate() == null) {
            return null;
        }
        List<RouteCoordinate> coordinates = source.coordinate().stream()
                .filter(Objects::nonNull)
                .filter(point -> point.x() != null && point.y() != null)
                .filter(point -> isLongitude(point.x()) && isLatitude(point.y()))
                .map(point -> new RouteCoordinate(point.x(), point.y()))
                .toList();
        return coordinates.size() >= 2
                ? new RoutePath(RouteSegmentType.WALK, coordinates)
                : null;
    }

    private RouteSegment toSegment(
            OdsayWalkResponse.Route source,
            List<OdsayWalkResponse.Guide> guides,
            int routeIndex
    ) {
        String instruction = null;
        if (guides != null) {
            instruction = guides.stream()
                    .filter(guide -> guide.routeIndex() != null
                            && guide.routeIndex() == routeIndex)
                    .map(OdsayWalkResponse.Guide::guidance)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return new RouteSegment(
                RouteSegmentType.WALK,
                null,
                null,
                null,
                source.duration() == null ? null : (long) Math.max(0, source.duration()),
                source.distance() == null ? null : (long) Math.max(0, source.distance()),
                null,
                instruction);
    }

    private RouteApiException pathError(String code) {
        if ("403".equals(code)) {
            return new RouteApiException(
                    "SAME_LOCATION", "출발지와 목적지가 같습니다.", HttpStatus.BAD_REQUEST);
        }
        if ("401".equals(code) || "402".equals(code)) {
            return invalidRequest("도보 경로 좌표를 확인해 주세요.");
        }
        return routeNotFound();
    }

    private RouteApiException providerException(OdsayWalkResponse.Error error) {
        String description = error.description() == null ? "" : error.description();
        if (description.contains("ApiKeyAuthFailed")) {
            return new RouteApiException(
                    "ODSAY_AUTHENTICATION_FAILED",
                    "ODsay API Key와 등록된 서버 IP를 확인해 주세요.",
                    HttpStatus.BAD_GATEWAY);
        }
        return new RouteApiException(
                "WALK_API_UNAVAILABLE",
                "현재 ODsay 상품 또는 권한으로 도보 길찾기를 사용할 수 없습니다.",
                HttpStatus.BAD_GATEWAY);
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

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private boolean isLongitude(double value) {
        return Double.isFinite(value) && value >= -180 && value <= 180;
    }

    private boolean isLatitude(double value) {
        return Double.isFinite(value) && value >= -90 && value <= 90;
    }

    private RouteApiException routeNotFound() {
        return new RouteApiException(
                "WALK_ROUTE_NOT_FOUND",
                "선택한 출발지와 목적지 사이의 도보 경로를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND);
    }

    private RouteApiException invalidResponse() {
        return new RouteApiException(
                "WALK_API_INVALID_RESPONSE",
                "도보 경로 응답을 처리하지 못했습니다.",
                HttpStatus.BAD_GATEWAY);
    }

    private RouteApiException invalidRequest(String message) {
        return new RouteApiException("INVALID_ROUTE_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
}
