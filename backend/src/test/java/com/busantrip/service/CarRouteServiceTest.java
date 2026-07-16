package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.client.NaverDirectionsClient;
import com.busantrip.dto.external.route.NaverDirectionsResponse;
import com.busantrip.dto.route.RouteResponse;
import com.busantrip.dto.route.RouteSegmentType;
import com.busantrip.exception.RouteApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class CarRouteServiceTest {

    private final NaverDirectionsClient client = mock(NaverDirectionsClient.class);
    private final CarRouteService service = new CarRouteService(client);

    @Test
    void convertsDirectionsPathSummaryAndGuidesToCommonResponse() {
        when(client.getDrivingRoute(
                129.0756, 35.1795, 129.1604, 35.1587, "traoptimal"))
                .thenReturn(Mono.just(successResponse()));

        RouteResponse result = service.findRoute(
                "CAR", 129.0756, 35.1795, 129.1604, 35.1587).block();

        assertThat(result).isNotNull();
        assertThat(result.mode().name()).isEqualTo("CAR");
        assertThat(result.summary().distanceMeters()).isEqualTo(12_345);
        assertThat(result.summary().durationSeconds()).isEqualTo(1_801);
        assertThat(result.summary().tollFare()).isEqualTo(1_000);
        assertThat(result.path()).hasSize(1);
        assertThat(result.path().getFirst().type()).isEqualTo(RouteSegmentType.DRIVE);
        assertThat(result.path().getFirst().coordinates().getFirst())
                .extracting("longitude", "latitude")
                .containsExactly(129.0756, 35.1795);
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().getFirst().instruction()).isEqualTo("우회전");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void rejectsInvalidAndSameCoordinatesBeforeCallingProvider() {
        Throwable invalid = catchThrowable(() -> service.findRoute(
                "CAR", 181.0, 35.0, 129.0, 35.1).block());
        Throwable same = catchThrowable(() -> service.findRoute(
                "CAR", 129.0, 35.0, 129.0, 35.0).block());

        assertThat(invalid).isInstanceOf(RouteApiException.class);
        assertThat(((RouteApiException) invalid).getCode())
                .isEqualTo("INVALID_ROUTE_REQUEST");
        assertThat(same).isInstanceOf(RouteApiException.class);
        assertThat(((RouteApiException) same).getCode()).isEqualTo("SAME_LOCATION");
    }

    @Test
    void returnsNotFoundInsteadOfCreatingStraightLineFallback() {
        when(client.getDrivingRoute(
                129.0756, 35.1795, 129.1604, 35.1587, "traoptimal"))
                .thenReturn(Mono.just(new NaverDirectionsResponse(3, "경로 없음", Map.of())));

        Throwable error = catchThrowable(() -> service.findRoute(
                "CAR", 129.0756, 35.1795, 129.1604, 35.1587).block());

        assertThat(error).isInstanceOf(RouteApiException.class);
        assertThat(((RouteApiException) error).getCode()).isEqualTo("ROUTE_NOT_FOUND");
    }

    private NaverDirectionsResponse successResponse() {
        NaverDirectionsResponse.Summary summary = new NaverDirectionsResponse.Summary(
                12_345, 1_800_500, 1_000, 18_000, 2_100);
        NaverDirectionsResponse.Guide guide = new NaverDirectionsResponse.Guide(
                1, "우회전", 400, 60_000);
        NaverDirectionsResponse.Route route = new NaverDirectionsResponse.Route(
                summary,
                List.of(
                        List.of(129.0756, 35.1795),
                        List.of(129.1604, 35.1587)),
                List.of(guide));
        return new NaverDirectionsResponse(
                0,
                "길찾기 성공",
                Map.of("traoptimal", List.of(route)));
    }
}
