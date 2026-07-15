package com.busantrip.controller;

import com.busantrip.dto.response.TourismResponse;
import com.busantrip.service.TourismService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tourism")
public class TourismController {

    private final TourismService tourismService;

    public TourismController(TourismService tourismService) {
        this.tourismService = tourismService;
    }

    /**
     * 부산 기초지자체 중심 관광지 조회
     */
    @GetMapping("/local")
    public Mono<TourismResponse> getLocalTourism(
            @RequestParam String baseYm,
            @RequestParam String signguCd,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows
    ) {
        return tourismService.getLocalTourism(
                baseYm,
                signguCd,
                pageNo,
                numOfRows
        );
    }

    /**
     * 특정 지역의 연관 관광지 조회
     */
    @GetMapping("/related")
    public Mono<TourismResponse> getRelatedTourismByArea(
            @RequestParam String baseYm,
            @RequestParam String signguCd,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows
    ) {
        return tourismService.getRelatedTourismByArea(
                baseYm,
                signguCd,
                pageNo,
                numOfRows
        );
    }

    /**
     * 관광지 이름으로 연관 관광지 검색
     */
    @GetMapping("/related/search")
    public Mono<TourismResponse> searchRelatedTourism(
            @RequestParam String baseYm,
            @RequestParam String signguCd,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows
    ) {
        return tourismService.searchRelatedTourism(
                baseYm,
                signguCd,
                keyword,
                pageNo,
                numOfRows
        );
    }
}
