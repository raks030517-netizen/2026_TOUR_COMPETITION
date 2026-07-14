package com.busantrip.controller;

import com.busantrip.client.RelatedTourismClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test/tourism/related")
public class RelatedTourismTestController {

    private final RelatedTourismClient relatedTourismClient;

    public RelatedTourismTestController(
            RelatedTourismClient relatedTourismClient
    ) {
        this.relatedTourismClient = relatedTourismClient;
    }

    @GetMapping("/area")
    public Mono<String> getByArea(
            @RequestParam String baseYm,
            @RequestParam String signguCd,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows
    ) {
        return relatedTourismClient.getRelatedTourismByArea(
                baseYm,
                signguCd,
                pageNo,
                numOfRows
        );
    }

    @GetMapping("/search")
    public Mono<String> search(
            @RequestParam String baseYm,
            @RequestParam String signguCd,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int numOfRows
    ) {
        return relatedTourismClient.searchRelatedTourism(
                baseYm,
                signguCd,
                keyword,
                pageNo,
                numOfRows
        );
    }
}