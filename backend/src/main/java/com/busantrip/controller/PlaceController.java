package com.busantrip.controller;

import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.service.NaverLocalSearchService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final NaverLocalSearchService placeSearchService;

    public PlaceController(NaverLocalSearchService placeSearchService) {
        this.placeSearchService = placeSearchService;
    }

    @GetMapping("/search")
    public Mono<List<PlaceResponse>> search(
            @RequestParam @NotBlank(message = "검색어를 입력해 주세요.") String query
    ) {
        return placeSearchService.search(query);
    }
}
