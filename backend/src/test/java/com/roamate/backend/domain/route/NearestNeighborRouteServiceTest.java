package com.roamate.backend.domain.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.roamate.backend.domain.place.Place;
import java.util.List;
import org.junit.jupiter.api.Test;

class NearestNeighborRouteServiceTest {

    private final NearestNeighborRouteService service = new NearestNeighborRouteService();

    @Test
    void 출발점에서_가까운_순서로_정렬한다() {
        Place near = place("near", 1.0);
        Place mid = place("mid", 3.0);
        Place far = place("far", 5.0);

        List<Place> ordered = service.optimizeOrder(List.of(far, near, mid), 0.0, 0.0);

        assertThat(ordered).extracting(Place::getName)
                .containsExactly("near", "mid", "far");
    }

    @Test
    void 시작좌표가_없으면_첫번째_장소를_기준으로_정렬한다() {
        Place first = place("first", 10.0);
        Place near = place("near", 11.0);
        Place far = place("far", 20.0);

        List<Place> ordered = service.optimizeOrder(List.of(first, far, near), null, null);

        assertThat(ordered).extracting(Place::getName)
                .containsExactly("first", "near", "far");
    }

    private Place place(String name, double latitude) {
        return Place.builder()
                .contentId(name + "-id")
                .name(name)
                .latitude(latitude)
                .longitude(0.0)
                .build();
    }
}
