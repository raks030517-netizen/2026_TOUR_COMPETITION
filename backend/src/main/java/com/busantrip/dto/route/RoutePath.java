package com.busantrip.dto.route;

import java.util.List;

public record RoutePath(
        RouteSegmentType type,
        List<RouteCoordinate> coordinates
) {
}
