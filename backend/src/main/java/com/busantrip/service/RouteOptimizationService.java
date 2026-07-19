package com.busantrip.service;

import com.busantrip.controller.RouteController.Coordinate;
import com.busantrip.controller.RouteController.Guide;
import com.busantrip.controller.RouteController.Place;
import com.busantrip.controller.RouteController.RouteRequest;
import com.busantrip.controller.RouteController.RouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RouteOptimizationService {

    private static final String DEFAULT_OPTION = "trafast";

    private final WebClient directionsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String keyId;
    private final String keySecret;

    public RouteOptimizationService(
            WebClient.Builder webClientBuilder,
            @Value("${external-api.naver-directions.base-url:https://maps.apigw.ntruss.com}") String baseUrl,
            @Value("${external-api.naver-directions.key-id:}") String keyId,
            @Value("${external-api.naver-directions.key-secret:}") String keySecret
    ) {
        this.directionsClient = webClientBuilder.baseUrl(baseUrl.trim()).build();
        this.keyId = keyId.trim();
        this.keySecret = keySecret.trim();
    }

    public Mono<RouteResponse> optimize(RouteRequest request) {
        if (request == null || request.start() == null || request.places() == null || request.places().isEmpty()) {
            return Mono.error(new IllegalArgumentException("출발지와 한 곳 이상의 장소가 필요합니다."));
        }

        List<Place> orderedPlaces = twoOpt(nearestNeighbor(request.start(), request.places()), request.start());
        String option = request.option() == null || request.option().isBlank() ? DEFAULT_OPTION : request.option();

        if (keyId.isBlank() || keySecret.isBlank()) {
            return Mono.just(fallback(request.start(), orderedPlaces, option));
        }

        return callAllDirections(request.start(), orderedPlaces, option)
                .onErrorReturn(fallback(request.start(), orderedPlaces, option));
    }

    private Mono<RouteResponse> callAllDirections(Coordinate start, List<Place> orderedPlaces, String option) {
        List<Mono<RouteResponse>> calls = new ArrayList<>();
        Coordinate segmentStart = start;
        int index = 0;

        while (index < orderedPlaces.size()) {
            int end = Math.min(orderedPlaces.size(), index + 16);
            List<Place> segmentPlaces = new ArrayList<>(orderedPlaces.subList(index, end));
            calls.add(callDirections(segmentStart, segmentPlaces, option));
            Place lastPlace = segmentPlaces.get(segmentPlaces.size() - 1);
            segmentStart = new Coordinate(lastPlace.latitude(), lastPlace.longitude());
            index = end;
        }

        return Mono.zip(calls, values -> {
            List<Coordinate> path = new ArrayList<>();
            List<Guide> guides = new ArrayList<>();
            long totalMeters = 0;
            long totalDuration = 0;
            int offset = 0;

            for (Object value : values) {
                RouteResponse segment = (RouteResponse) value;
                List<Coordinate> segmentPath = new ArrayList<>(segment.path());
                if (!path.isEmpty() && !segmentPath.isEmpty()) {
                    segmentPath.removeFirst();
                }
                path.addAll(segmentPath);
                for (Guide guide : segment.guides()) {
                    guides.add(new Guide(
                            guide.instruction(),
                            guide.distanceMeters(),
                            guide.durationMillis(),
                            guide.pointIndex() + offset
                    ));
                }
                offset = Math.max(0, path.size() - 1);
                totalMeters += segment.totalDistanceMeters();
                totalDuration += segment.totalDurationMillis();
            }

            return new RouteResponse(orderedPlaces, path, guides, totalMeters, totalDuration, false, option);
        });
    }

    private Mono<RouteResponse> callDirections(Coordinate start, List<Place> orderedPlaces, String option) {
        Place goal = orderedPlaces.get(orderedPlaces.size() - 1);
        String waypoints = orderedPlaces.size() <= 1 ? null : orderedPlaces.subList(0, orderedPlaces.size() - 1).stream()
                .map(place -> place.longitude() + "," + place.latitude())
                .reduce((left, right) -> left + "|" + right)
                .orElse(null);

        return directionsClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/map-direction-15/v1/driving")
                            .queryParam("start", start.longitude() + "," + start.latitude())
                            .queryParam("goal", goal.longitude() + "," + goal.latitude())
                            .queryParam("option", option);
                    if (waypoints != null) {
                        builder.queryParam("waypoints", waypoints);
                    }
                    return builder.build();
                })
                .header("x-ncp-apigw-api-key-id", keyId)
                .header("x-ncp-apigw-api-key", keySecret)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> parseDirections(response, orderedPlaces, option));
    }

    private RouteResponse parseDirections(String json, List<Place> orderedPlaces, String option) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode route = root.path("route");
            JsonNode routes = route.elements().hasNext() ? route.elements().next() : null;
            if (routes == null || !routes.isArray() || routes.isEmpty()) {
                throw new IllegalStateException("경로 결과가 비어 있습니다.");
            }

            JsonNode first = routes.get(0);
            JsonNode summary = first.path("summary");
            List<Coordinate> path = new ArrayList<>();
            first.path("path").forEach(point -> path.add(new Coordinate(point.get(1).asDouble(), point.get(0).asDouble())));

            List<Guide> guides = new ArrayList<>();
            first.path("guide").forEach(guide -> guides.add(new Guide(
                    guide.path("instructions").asText("경로를 따라 이동해 주세요."),
                    guide.path("distance").asLong(),
                    guide.path("duration").asLong(),
                    guide.path("pointIndex").asInt()
            )));
            return new RouteResponse(
                    orderedPlaces,
                    path,
                    guides,
                    summary.path("distance").asLong(),
                    summary.path("duration").asLong(),
                    false,
                    option
            );
        } catch (Exception exception) {
            throw new IllegalStateException("길찾기 API 응답을 해석하지 못했습니다.", exception);
        }
    }

    private List<Place> nearestNeighbor(Coordinate start, List<Place> places) {
        List<Place> remaining = new ArrayList<>(places);
        List<Place> ordered = new ArrayList<>();
        double latitude = start.latitude();
        double longitude = start.longitude();

        while (!remaining.isEmpty()) {
            Place nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Place candidate : remaining) {
                double distance = haversine(latitude, longitude, candidate.latitude(), candidate.longitude());
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
            ordered.add(nearest);
            remaining.remove(nearest);
            latitude = nearest.latitude();
            longitude = nearest.longitude();
        }
        return ordered;
    }

    private List<Place> twoOpt(List<Place> initial, Coordinate start) {
        List<Place> best = new ArrayList<>(initial);
        double bestDistance = totalDistance(start, best);
        boolean improved = true;
        int iteration = 0;

        while (improved && iteration++ < 25) {
            improved = false;
            for (int first = 0; first < best.size() - 1; first++) {
                for (int second = first + 1; second < best.size(); second++) {
                    List<Place> candidate = new ArrayList<>(best);
                    Collections.reverse(candidate.subList(first, second + 1));
                    double candidateDistance = totalDistance(start, candidate);
                    if (candidateDistance + 0.001 < bestDistance) {
                        best = candidate;
                        bestDistance = candidateDistance;
                        improved = true;
                    }
                }
            }
        }
        return best;
    }

    private double totalDistance(Coordinate start, List<Place> places) {
        double distance = 0;
        double latitude = start.latitude();
        double longitude = start.longitude();
        for (Place place : places) {
            distance += haversine(latitude, longitude, place.latitude(), place.longitude());
            latitude = place.latitude();
            longitude = place.longitude();
        }
        return distance;
    }

    private double haversine(double latitude1, double longitude1, double latitude2, double longitude2) {
        double radius = 6_371.0088;
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private RouteResponse fallback(Coordinate start, List<Place> orderedPlaces, String option) {
        List<Coordinate> path = new ArrayList<>();
        path.add(start);
        long meters = 0;
        double latitude = start.latitude();
        double longitude = start.longitude();

        for (Place place : orderedPlaces) {
            path.add(new Coordinate(place.latitude(), place.longitude()));
            meters += Math.round(haversine(latitude, longitude, place.latitude(), place.longitude()) * 1_000);
            latitude = place.latitude();
            longitude = place.longitude();
        }

        long duration = Math.round(meters / 8.5 * 1_000);
        Guide guide = new Guide("도로 길찾기 API가 설정되지 않아 직선거리 기준의 예상 경로를 표시합니다.", meters, duration, 0);
        return new RouteResponse(orderedPlaces, path, List.of(guide), meters, duration, true, option);
    }
}
