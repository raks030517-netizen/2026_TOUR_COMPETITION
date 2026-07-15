package com.busantrip.controller;

import com.busantrip.dto.traffic.AviTrafficResponse;
import com.busantrip.service.AviTrafficService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final AviTrafficService aviTrafficService;

    public TrafficController(AviTrafficService aviTrafficService) {
        this.aviTrafficService = aviTrafficService;
    }

    @GetMapping("/avi")
    public Mono<List<AviTrafficResponse>> getAviTraffic() {
        return aviTrafficService.getTraffic();
    }
}
