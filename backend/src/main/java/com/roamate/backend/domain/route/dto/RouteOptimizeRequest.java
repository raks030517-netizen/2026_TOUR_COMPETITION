package com.roamate.backend.domain.route.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RouteOptimizeRequest(
        @NotEmpty List<Long> placeIds,
        Double startLatitude,
        Double startLongitude
) {
}
