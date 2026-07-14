package com.roamate.backend.domain.route;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.domain.route.dto.RouteOptimizeRequest;
import com.roamate.backend.domain.route.dto.RouteStopResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/optimize")
    public ApiResponse<List<RouteStopResponse>> optimize(@Valid @RequestBody RouteOptimizeRequest request) {
        return ApiResponse.ok(routeService.optimize(request));
    }
}
