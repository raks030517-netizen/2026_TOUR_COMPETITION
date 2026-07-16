package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.client.OdsayTransitClient;
import com.busantrip.dto.external.route.OdsayTransitResponse;
import com.busantrip.dto.route.RouteResponse;
import com.busantrip.dto.route.RouteSegmentType;
import com.busantrip.exception.RouteApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class TransitRouteServiceTest {

    private final OdsayTransitClient client = mock(OdsayTransitClient.class);
    private final TransitRouteService service = new TransitRouteService(client);

    @Test
    void convertsRecommendedRoutesSummarySegmentsAndActualStopCoordinates() {
        when(client.findRoutes(129.0756, 35.1795, 129.1604, 35.1587))
                .thenReturn(Mono.just(successResponse()));

        List<RouteResponse> routes = service.findRoutes(
                129.0756, 35.1795, 129.1604, 35.1587).block();

        assertThat(routes).hasSize(2);
        RouteResponse route = routes.getFirst();
        assertThat(route.mode().name()).isEqualTo("TRANSIT");
        assertThat(route.summary().distanceMeters()).isEqualTo(11_235);
        assertThat(route.summary().durationSeconds()).isEqualTo(2_580);
        assertThat(route.summary().fare()).isEqualTo(1_550);
        assertThat(route.summary().walkingDistanceMeters()).isEqualTo(710);
        assertThat(route.summary().transferCount()).isEqualTo(1);
        assertThat(route.segments()).extracting("type")
                .containsExactly(RouteSegmentType.WALK, RouteSegmentType.BUS, RouteSegmentType.SUBWAY);
        assertThat(route.segments().get(1).transportName()).isEqualTo("1003");
        assertThat(route.segments().get(1).startName()).isEqualTo("부산역");
        assertThat(route.path()).hasSize(2);
        assertThat(route.path().getFirst().type()).isEqualTo(RouteSegmentType.BUS);
        assertThat(route.path().getFirst().coordinates().getFirst())
                .extracting("longitude", "latitude")
                .containsExactly(129.0756, 35.1795);
        assertThat(route.path()).noneMatch(path -> path.type() == RouteSegmentType.WALK);
    }

    @Test
    void keepsPathEmptyInsteadOfCreatingStraightLineWhenStopsHaveNoCoordinates() {
        OdsayTransitResponse.Path path = new OdsayTransitResponse.Path(
                2,
                info(10, 1200.0),
                List.of(new OdsayTransitResponse.SubPath(
                        2, 1200.0, 10, 4, List.of(new OdsayTransitResponse.Lane(null, "40")),
                        "A", 129.0, 35.0, "B", 129.1, 35.1, null)));
        when(client.findRoutes(129.0, 35.0, 129.1, 35.1))
                .thenReturn(Mono.just(new OdsayTransitResponse(
                        new OdsayTransitResponse.Result(0, List.of(path)), null)));

        RouteResponse route = service.findRoutes(129.0, 35.0, 129.1, 35.1).block().getFirst();

        assertThat(route.path()).isEmpty();
        assertThat(route.warnings()).isNotEmpty();
    }

    @Test
    void mapsAuthenticationAndNoRouteErrorsToSanitizedCodes() {
        when(client.findRoutes(129.0, 35.0, 129.1, 35.1))
                .thenReturn(Mono.just(new OdsayTransitResponse(
                        null,
                        List.of(new OdsayTransitResponse.Error(
                                500, null, "[ApiKeyAuthFailed] ApiKey authentication failed.")))));
        Throwable authentication = catchThrowable(() -> service.findRoutes(
                129.0, 35.0, 129.1, 35.1).block());

        when(client.findRoutes(129.0, 35.0, 129.1, 35.1))
                .thenReturn(Mono.just(new OdsayTransitResponse(
                        null, List.of(new OdsayTransitResponse.Error(-99, "검색결과가 없습니다.", null)))));
        Throwable notFound = catchThrowable(() -> service.findRoutes(
                129.0, 35.0, 129.1, 35.1).block());

        assertThat(((RouteApiException) authentication).getCode())
                .isEqualTo("ODSAY_AUTHENTICATION_FAILED");
        assertThat(authentication.getMessage()).doesNotContain("ApiKeyAuthFailed");
        assertThat(((RouteApiException) notFound).getCode()).isEqualTo("TRANSIT_ROUTE_NOT_FOUND");
    }

    private OdsayTransitResponse successResponse() {
        OdsayTransitResponse.SubPath walk = new OdsayTransitResponse.SubPath(
                3, 240.0, 4, null, null, null, null, null, null, null, null, null);
        OdsayTransitResponse.SubPath bus = new OdsayTransitResponse.SubPath(
                2, 8_400.0, 25, 15,
                List.of(new OdsayTransitResponse.Lane(null, "1003")),
                "부산역", 129.0756, 35.1795, "서면역", 129.0590, 35.1578,
                stops(
                        station(0, "부산역", "129.0756", "35.1795"),
                        station(1, "서면역", "129.0590", "35.1578")));
        OdsayTransitResponse.SubPath subway = new OdsayTransitResponse.SubPath(
                1, 2_595.0, 14, 5,
                List.of(new OdsayTransitResponse.Lane("부산 2호선", null)),
                "서면역", 129.0590, 35.1578, "해운대역", 129.1589, 35.1630,
                stops(
                        station(0, "서면역", 129.0590, 35.1578),
                        station(1, "해운대역", 129.1589, 35.1630)));
        OdsayTransitResponse.Path first = new OdsayTransitResponse.Path(
                3, info(43, 11_234.5), List.of(walk, bus, subway));
        OdsayTransitResponse.Path second = new OdsayTransitResponse.Path(
                2, info(48, 12_000.0), List.of(walk, bus));
        return new OdsayTransitResponse(
                new OdsayTransitResponse.Result(0, List.of(first, second)), null);
    }

    private OdsayTransitResponse.Info info(int minutes, double distance) {
        return new OdsayTransitResponse.Info(
                distance - 710, 710, minutes, 1_550, 1, 1,
                "부산역", "해운대역", distance);
    }

    private OdsayTransitResponse.PassStopList stops(OdsayTransitResponse.Station... stations) {
        return new OdsayTransitResponse.PassStopList(List.of(stations));
    }

    private OdsayTransitResponse.Station station(int index, String name, Object x, Object y) {
        return new OdsayTransitResponse.Station(index, name, x, y);
    }
}
