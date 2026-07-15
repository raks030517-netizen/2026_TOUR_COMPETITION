package com.busantrip.controller;

import com.busantrip.dto.external.weather.WeatherApiResponse;
import com.busantrip.dto.response.WeatherResponse;
import com.busantrip.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 기상청 단기예보 조회
     *
     * 요청 예시:
     * GET /api/weather/forecast
     *     ?baseDate=20260714
     *     &baseTime=0500
     *     &nx=98
     *     &ny=76
     */
    @GetMapping("/forecast")
    public Mono<WeatherResponse> getForecast(
            @RequestParam String baseDate,
            @RequestParam String baseTime,
            @RequestParam int nx,
            @RequestParam int ny,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "1000") int numOfRows
    ) {
        return weatherService.getForecast(
                baseDate,
                baseTime,
                nx,
                ny,
                pageNo,
                numOfRows
        );
    }
}