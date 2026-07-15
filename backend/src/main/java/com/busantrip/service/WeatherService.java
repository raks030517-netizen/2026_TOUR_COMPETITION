package com.busantrip.service;

import com.busantrip.client.WeatherClient;
import com.busantrip.dto.external.weather.WeatherApiResponse;
import com.busantrip.dto.response.WeatherResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class WeatherService {

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