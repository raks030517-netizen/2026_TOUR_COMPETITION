package com.busantrip.controller;

import com.busantrip.service.RouteOptimizationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteOptimizationService service;
    public RouteController(RouteOptimizationService service){this.service=service;}
    @PostMapping("/optimize")
    public Mono<RouteResponse> optimize(@RequestBody RouteRequest request){return service.optimize(request);}
    public record Coordinate(double latitude,double longitude){}
    public record Place(String id,String name,String description,String address,String image,String category,String distance,double latitude,double longitude){}
    public record RouteRequest(Coordinate start,List<Place> places,String option){}
    public record Guide(String instruction,long distanceMeters,long durationMillis,int pointIndex){}
    public record RouteResponse(List<Place> orderedPlaces,List<Coordinate> path,List<Guide> guides,long totalDistanceMeters,long totalDurationMillis,boolean fallback,String option){}
}
