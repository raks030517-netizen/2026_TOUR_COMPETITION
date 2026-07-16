package com.busantrip.controller;

import com.busantrip.dto.route.RouteResponse;
import com.busantrip.service.CarRouteService;
import com.busantrip.service.TransitRouteService;
import com.busantrip.service.WalkRouteService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final CarRouteService carRouteService;
    private final TransitRouteService transitRouteService;
    private final WalkRouteService walkRouteService;

    public RouteController(
            CarRouteService carRouteService,
            TransitRouteService transitRouteService,
            WalkRouteService walkRouteService
    ) {
        this.carRouteService = carRouteService;
        this.transitRouteService = transitRouteService;
        this.walkRouteService = walkRouteService;
    }

    @GetMapping
    public Mono<?> findRoute(
            @RequestParam String mode,
            @RequestParam Double startLng,
            @RequestParam Double startLat,
            @RequestParam Double endLng,
            @RequestParam Double endLat
    ) {
        return switch (mode.trim().toUpperCase()) {
            case "TRANSIT" -> transitRouteService.findRoutes(startLng, startLat, endLng, endLat);
            case "WALK" -> walkRouteService.findRoute(startLng, startLat, endLng, endLat);
            default -> carRouteService.findRoute(mode, startLng, startLat, endLng, endLat);
        };
    }
}
