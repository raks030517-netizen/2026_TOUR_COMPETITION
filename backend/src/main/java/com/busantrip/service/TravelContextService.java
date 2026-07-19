package com.busantrip.service;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.itinerary.ItineraryRequest;
import com.busantrip.dto.itinerary.TravelContext;
import com.busantrip.dto.response.WeatherResponse;
import com.busantrip.dto.traffic.AviTrafficResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 날씨와 교통은 언어 모델이 추측하면 안 되는 사실 데이터다. 키 또는 조회 가능 기간이
 * 없는 경우에는 데이터 상태를 명확히 표시한 데모 신호를 반환한다.
 */
@Service
public class TravelContextService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int BUSAN_NX = 98;
    private static final int BUSAN_NY = 76;

    private final WeatherService weatherService;
    private final AviTrafficService aviTrafficService;
    private final ApiKeyProperties apiKeyProperties;

    public TravelContextService(
            WeatherService weatherService,
            AviTrafficService aviTrafficService,
            ApiKeyProperties apiKeyProperties
    ) {
        this.weatherService = weatherService;
        this.aviTrafficService = aviTrafficService;
        this.apiKeyProperties = apiKeyProperties;
    }

    public Mono<TravelContext> load(ItineraryRequest request) {
        return Mono.zip(weather(request.startDate()), traffic())
                .map(values -> {
                    TravelContext.Signal weather = values.getT1();
                    TravelContext.Signal traffic = values.getT2();
                    List<String> alerts = new ArrayList<>();
                    if (weather.detail().contains("강수확률") && weather.detail().matches(".*강수확률 (6[0-9]|[7-9][0-9]|100)%.*")) {
                        alerts.add("비 예보가 있어 실내 대체 장소와 우산을 함께 확인하세요.");
                    }
                    if (!weather.live()) {
                        alerts.add("날씨는 데모 상태입니다. 출발 전 실제 예보를 다시 확인하세요.");
                    }
                    if (!traffic.live()) {
                        alerts.add("교통량은 데모 상태입니다. 출발 직전 길찾기 결과를 확인하세요.");
                    }
                    return new TravelContext(weather, traffic, List.copyOf(alerts));
                });
    }

    private Mono<TravelContext.Signal> weather(LocalDate travelDate) {
        LocalDate today = LocalDate.now(SEOUL);
        if (!configured(apiKeyProperties.getWeather().getServiceKey())) {
            return Mono.just(demoWeather("기상청 키가 없어 데모 날씨 상태를 표시합니다."));
        }
        if (travelDate.isBefore(today) || travelDate.isAfter(today.plusDays(3))) {
            return Mono.just(demoWeather("단기예보 제공 기간 밖입니다. 출발 전 날씨를 확인하세요."));
        }

        return weatherService.getForecast(today.format(DATE), "0500", BUSAN_NX, BUSAN_NY, 1, 300)
                .map(this::toWeatherSignal)
                .onErrorReturn(demoWeather("기상청 예보를 불러오지 못해 데모 상태로 전환했습니다."));
    }

    private TravelContext.Signal toWeatherSignal(WeatherResponse weather) {
        String temperature = weather.temperature() == null ? "기온 정보 없음" : Math.round(weather.temperature()) + "°C";
        String precipitation = weather.precipitationProbability() == null
                ? "강수확률 정보 없음"
                : "강수확률 " + weather.precipitationProbability() + "%";
        String condition = weather.precipitationType() != null && !weather.precipitationType().equals("없음")
                ? weather.precipitationType()
                : valueOr(weather.skyCondition(), "날씨 정보");
        return new TravelContext.Signal("기상청 · " + condition, temperature + " · " + precipitation, true);
    }

    private Mono<TravelContext.Signal> traffic() {
        if (!configured(apiKeyProperties.getAvi().getServiceKey())) {
            return Mono.just(demoTraffic("부산 ITS 키가 없어 데모 교통 상태를 표시합니다."));
        }

        return aviTrafficService.getTraffic()
                .map(this::toTrafficSignal)
                .onErrorReturn(demoTraffic("부산 ITS 교통량을 불러오지 못해 데모 상태로 전환했습니다."));
    }

    private TravelContext.Signal toTrafficSignal(List<AviTrafficResponse> responses) {
        if (responses.isEmpty()) {
            return new TravelContext.Signal("부산 ITS · 관측 정보 없음", "운전 경로는 출발 직전에 다시 확인하세요.", true);
        }
        long average = Math.round(responses.stream().mapToLong(AviTrafficResponse::trafficVolume).average().orElse(0));
        String label = average >= 1_500 ? "부산 ITS · 혼잡 주의" : "부산 ITS · 이동 가능";
        return new TravelContext.Signal(label, responses.size() + "개 지점 평균 교통량 " + average, true);
    }

    private TravelContext.Signal demoWeather(String detail) {
        return new TravelContext.Signal("날씨 · 확인 필요", detail, false);
    }

    private TravelContext.Signal demoTraffic(String detail) {
        return new TravelContext.Signal("교통 · 확인 필요", detail, false);
    }

    private boolean configured(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
