package com.busantrip.client;

import com.busantrip.config.ApiKeyProperties;
import com.busantrip.dto.external.weather.WeatherApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WeatherClient {

    private static final String VILLAGE_FORECAST_PATH = "/getVilageFcst";
    private static final String DATA_TYPE = "JSON";

    private final WebClient webClient;
    private final String serviceKey;

    public WeatherClient(
            WebClient.Builder webClientBuilder,
            ApiKeyProperties apiKeyProperties
    ) {
        ApiKeyProperties.Weather weatherProperties =
                apiKeyProperties.getWeather();

        this.webClient = webClientBuilder
                .baseUrl(weatherProperties.getBaseUrl())
                .build();

        this.serviceKey = weatherProperties
                .getServiceKey()
                .trim();
    }

    /**
     * 기상청 단기예보를 조회한다.
     * @param baseDate  발표일자(yyyyMMdd)
     * @param baseTime  발표시각(HHmm)
     * @param nx        예보지점 X 좌표
     * @param ny        예보지점 Y 좌표
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return 기상청 API의 JSON 응답
     */
    public Mono<WeatherApiResponse> getForecast(
            String baseDate,
            String baseTime,
            int nx,
            int ny,
            int pageNo,
            int numOfRows
    ) {
        validateRequest(
                baseDate,
                baseTime,
                nx,
                ny,
                pageNo,
                numOfRows
        );

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(VILLAGE_FORECAST_PATH)

                        // 인증키에 포함된 +, /, = 문자의 중복 인코딩 방지
                        .queryParam("serviceKey", "{serviceKey}")

                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("dataType", DATA_TYPE)
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .build(serviceKey)
                )
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("응답 본문 없음")
                                .flatMap(errorBody -> Mono.error(
                                        new IllegalStateException(
                                                "기상청 단기예보 API 호출 실패: "
                                                        + response.statusCode()
                                                        + ", 응답: "
                                                        + errorBody
                                        )
                                ))
                )
                .bodyToMono(WeatherApiResponse.class);
    }

    private void validateRequest(
            String baseDate,
            String baseTime,
            int nx,
            int ny,
            int pageNo,
            int numOfRows
    ) {
        if (serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "기상청 API 인증키가 설정되지 않았습니다."
            );
        }

        if (baseDate == null || !baseDate.matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "baseDate는 yyyyMMdd 형식이어야 합니다."
            );
        }

        if (baseTime == null || !baseTime.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "baseTime은 HHmm 형식이어야 합니다."
            );
        }

        if (nx <= 0 || ny <= 0) {
            throw new IllegalArgumentException(
                    "nx와 ny는 0보다 커야 합니다."
            );
        }

        if (pageNo <= 0 || numOfRows <= 0) {
            throw new IllegalArgumentException(
                    "pageNo와 numOfRows는 0보다 커야 합니다."
            );
        }
    }
}