package com.busantrip.controller;

import com.busantrip.dto.travel.TravelSearchRequest;
import com.busantrip.dto.travel.TravelSearchResponse;
import com.busantrip.service.TravelSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/travel")
public class TravelSearchController {

    private final TravelSearchService travelSearchService;

    public TravelSearchController(TravelSearchService travelSearchService) {
        this.travelSearchService = travelSearchService;
    }

    @PostMapping("/search")
    public Mono<TravelSearchResponse> search(@Valid @RequestBody TravelSearchRequest request) {
        return travelSearchService.search(request.message());
    }
}
