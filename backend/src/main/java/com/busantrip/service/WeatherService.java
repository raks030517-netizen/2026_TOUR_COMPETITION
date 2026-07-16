package com.busantrip.service;

import com.busantrip.client.WeatherClient;
import com.busantrip.dto.external.weather.WeatherApiResponse;
import com.busantrip.dto.response.WeatherResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class WeatherService {

    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    private final WeatherClient weatherClient;

    public WeatherService(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    /**
     * 기상청 단기예보를 조회하고 화면 및 AI용 요약 DTO로 변환한다.
     */
    public Mono<WeatherResponse> getForecast(
            String baseDate,
            String baseTime,
            int nx,
            int ny,
            int pageNo,
            int numOfRows
    ) {
        return weatherClient.getForecast(
                baseDate,
                baseTime,
                nx,
                ny,
                pageNo,
                numOfRows
        ).map(this::toWeatherResponse);
    }

    public Mono<WeatherResponse> getCurrentForecast(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < 32 || latitude > 39 || longitude < 123 || longitude > 133) {
            return Mono.error(new IllegalArgumentException("대한민국 범위의 위도와 경도를 입력해 주세요."));
        }
        int[] grid = toGrid(latitude, longitude);
        LocalDateTime availableAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(15);
        int baseHour = -1;
        for (int hour : BASE_HOURS) {
            if (hour <= availableAt.getHour()) baseHour = hour;
        }
        if (baseHour < 0) {
            availableAt = availableAt.minusDays(1);
            baseHour = 23;
        }
        String baseDate = availableAt.toLocalDate().toString().replace("-", "");
        String baseTime = "%02d00".formatted(baseHour);
        return getForecast(baseDate, baseTime, grid[0], grid[1], 1, 1000);
    }

    private int[] toGrid(double latitude, double longitude) {
        double re = 6371.00877 / 5.0;
        double slat1 = Math.toRadians(30.0);
        double slat2 = Math.toRadians(60.0);
        double olon = Math.toRadians(126.0);
        double olat = Math.toRadians(38.0);
        double sn = Math.log(Math.cos(slat1) / Math.cos(slat2))
                / Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn)
                * Math.cos(slat1) / sn;
        double ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);
        double ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + Math.toRadians(latitude) * 0.5), sn);
        double theta = Math.toRadians(longitude) - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;
        int x = (int) Math.floor(ra * Math.sin(theta) + 43.0 + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + 136.0 + 0.5);
        return new int[]{x, y};
    }

    /**
     * 기상청 원본 응답에서 첫 번째 예보 시각의 주요 날씨 항목을 추출한다.
     */
    private WeatherResponse toWeatherResponse(
            WeatherApiResponse apiResponse
    ) {
        WeatherApiResponse.Body body = apiResponse.response().body();

        if (body == null
                || body.items() == null
                || body.items().item() == null
                || body.items().item().isEmpty()) {
            throw new IllegalStateException("기상청 예보 데이터가 없습니다.");
        }

        List<WeatherApiResponse.Item> items = body.items().item();

        // 응답 목록에서 가장 먼저 등장하는 예보 날짜와 시각을 기준으로 묶는다.
        String forecastDate = items.get(0).fcstDate();
        String forecastTime = items.get(0).fcstTime();

        List<WeatherApiResponse.Item> targetItems = items.stream()
                .filter(item ->
                        forecastDate.equals(item.fcstDate())
                                && forecastTime.equals(item.fcstTime())
                )
                .toList();

        Double temperature = getDoubleValue(targetItems, "TMP");
        Integer precipitationProbability = getIntegerValue(targetItems, "POP");
        String precipitationType = convertPrecipitationType(
                getStringValue(targetItems, "PTY")
        );
        String skyCondition = convertSkyCondition(
                getStringValue(targetItems, "SKY")
        );
        Double windSpeed = getDoubleValue(targetItems, "WSD");
        Integer humidity = getIntegerValue(targetItems, "REH");

        return new WeatherResponse(
                forecastDate,
                forecastTime,
                temperature,
                precipitationProbability,
                precipitationType,
                skyCondition,
                windSpeed,
                humidity
        );
    }

    private String getStringValue(
            List<WeatherApiResponse.Item> items,
            String category
    ) {
        return items.stream()
                .filter(item -> category.equals(item.category()))
                .map(WeatherApiResponse.Item::fcstValue)
                .findFirst()
                .orElse(null);
    }

    private Double getDoubleValue(
            List<WeatherApiResponse.Item> items,
            String category
    ) {
        return parseDouble(getStringValue(items, category));
    }

    private Integer getIntegerValue(
            List<WeatherApiResponse.Item> items,
            String category
    ) {
        return parseInteger(getStringValue(items, category));
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 기상청 PTY 코드 변환
     */
    private String convertPrecipitationType(String value) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "알 수 없음";
        };
    }

    /**
     * 기상청 SKY 코드 변환
     */
    private String convertSkyCondition(String value) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알 수 없음";
        };
    }
}
