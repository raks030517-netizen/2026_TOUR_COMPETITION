package com.busantrip.service;

import com.busantrip.client.WeatherClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WeatherService {

    private final WeatherClient weatherClient;

    public WeatherService(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    /**
     * 기상청 단기예보를 조회한다.
     *
     * @param baseDate 발표일자(yyyyMMdd)
     * @param baseTime 발표시각(HHmm)
     * @param nx        기상청 격자 X 좌표
     * @param ny        기상청 격자 Y 좌표
     * @param pageNo    페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return 기상청 API의 원본 JSON 응답
     */
    public Mono<String> getForecast(
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
        );
    }
}