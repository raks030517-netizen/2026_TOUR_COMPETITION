package com.busantrip.controller;

import com.busantrip.dto.itinerary.ItineraryAdjustmentRequest;
import com.busantrip.dto.itinerary.ItineraryRequest;
import com.busantrip.dto.itinerary.ItineraryResponse;
import com.busantrip.service.ItineraryPlannerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryPlannerService itineraryPlannerService;

    public ItineraryController(ItineraryPlannerService itineraryPlannerService) {
        this.itineraryPlannerService = itineraryPlannerService;
    }

    @PostMapping("/plan")
    public Mono<ItineraryResponse> plan(@Valid @RequestBody ItineraryRequest request) {
        return itineraryPlannerService.plan(request);
    }

    @PostMapping("/adjust")
    public Mono<ItineraryResponse> adjust(@Valid @RequestBody ItineraryAdjustmentRequest request) {
        return itineraryPlannerService.adjust(request);
    }
}
