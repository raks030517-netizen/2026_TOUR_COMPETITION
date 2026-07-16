package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.client.OdsayWalkClient;
import com.busantrip.dto.external.route.OdsayWalkResponse;
import com.busantrip.dto.route.RouteResponse;
import com.busantrip.dto.route.RouteSegmentType;
import com.busantrip.exception.RouteApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class WalkRouteServiceTest {

    private final OdsayWalkClient client = mock(OdsayWalkClient.class);
    private final WalkRouteService service = new WalkRouteService(client);

    @Test
    void convertsSummaryGuidesAndOnlyActualCoordinates() {
        when(client.findRoute(129.0756, 35.1795, 129.0860, 35.1690))
                .thenReturn(Mono.just(successResponse(true)));

        RouteResponse route = service.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block();

        assertThat(route.mode().name()).isEqualTo("WALK");
        assertThat(route.summary().distanceMeters()).isEqualTo(1_540);
        assertThat(route.summary().durationSeconds()).isEqualTo(1_320);
        assertThat(route.summary().walkingDistanceMeters()).isEqualTo(1_540);
        assertThat(route.path()).hasSize(1);
        assertThat(route.path().getFirst().type()).isEqualTo(RouteSegmentType.WALK);
        assertThat(route.path().getFirst().coordinates().getFirst())
                .extracting("longitude", "latitude")
                .containsExactly(129.0756, 35.1795);
        assertThat(route.segments().getFirst().instruction()).isEqualTo("직진하세요.");
        assertThat(route.warnings()).isEmpty();
    }

    @Test
    void returnsSummaryWithoutFabricatingPathWhenCoordinatesAreMissing() {
        when(client.findRoute(129.0756, 35.1795, 129.0860, 35.1690))
                .thenReturn(Mono.just(successResponse(false)));

        RouteResponse route = service.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block();

        assertThat(route.summary().distanceMeters()).isEqualTo(1_540);
        assertThat(route.path()).isEmpty();
        assertThat(route.warnings()).containsExactly(
                "ODsay 응답에 실제 도보 경로 좌표가 없어 거리와 시간만 표시합니다.");
    }

    @Test
    void mapsProviderPermissionAndRouteErrors() {
        when(client.findRoute(129.0756, 35.1795, 129.0860, 35.1690))
                .thenReturn(Mono.just(new OdsayWalkResponse(
                        null,
                        List.of(new OdsayWalkResponse.Error(
                                500, null, "Not allowed product")))));
        Throwable unavailable = catchThrowable(() -> service.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block());

        OdsayWalkResponse.PathResult noPath = new OdsayWalkResponse.PathResult(
                false, "414", 4.0, null);
        when(client.findRoute(129.0756, 35.1795, 129.0860, 35.1690))
                .thenReturn(Mono.just(new OdsayWalkResponse(
                        new OdsayWalkResponse.Result(List.of(noPath)), null)));
        Throwable notFound = catchThrowable(() -> service.findRoute(
                129.0756, 35.1795, 129.0860, 35.1690).block());

        assertThat(((RouteApiException) unavailable).getCode()).isEqualTo("WALK_API_UNAVAILABLE");
        assertThat(((RouteApiException) notFound).getCode()).isEqualTo("WALK_ROUTE_NOT_FOUND");
    }

    @Test
    void validatesCoordinatesBeforeCallingProvider() {
        Throwable invalid = catchThrowable(() -> service.findRoute(
                181.0, 35.0, 129.0, 35.1).block());
        Throwable same = catchThrowable(() -> service.findRoute(
                129.0, 35.0, 129.0, 35.0).block());

        assertThat(((RouteApiException) invalid).getCode()).isEqualTo("INVALID_ROUTE_REQUEST");
        assertThat(((RouteApiException) same).getCode()).isEqualTo("SAME_LOCATION");
    }

    private OdsayWalkResponse successResponse(boolean withCoordinates) {
        List<OdsayWalkResponse.Point> coordinates = withCoordinates
                ? List.of(
                        new OdsayWalkResponse.Point(129.0756, 35.1795),
                        new OdsayWalkResponse.Point(129.0860, 35.1690))
                : List.of();
        OdsayWalkResponse.Route sourceRoute = new OdsayWalkResponse.Route(
                11, 0, 1_540, 1_320, coordinates);
        OdsayWalkResponse.Guide guide = new OdsayWalkResponse.Guide(
                0, 0, 0, 0, "직진하세요.",
                new OdsayWalkResponse.Point(129.0756, 35.1795));
        OdsayWalkResponse.WalkPath walkPath = new OdsayWalkResponse.WalkPath(
                new OdsayWalkResponse.Summary(
                        new OdsayWalkResponse.Point(129.0756, 35.1795),
                        new OdsayWalkResponse.Point(129.0860, 35.1690),
                        1_540, 1_320),
                List.of(sourceRoute),
                List.of(guide));
        return new OdsayWalkResponse(
                new OdsayWalkResponse.Result(List.of(
                        new OdsayWalkResponse.PathResult(true, null, 4.2, walkPath))),
                null);
    }
}
