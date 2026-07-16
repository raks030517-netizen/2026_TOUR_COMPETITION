package com.busantrip.dto.route;

import java.util.List;

public record RouteResponse(
        RouteMode mode,
        RouteSummary summary,
        List<RoutePath> path,
        List<RouteSegment> segments,
        List<String> warnings
) {
}
