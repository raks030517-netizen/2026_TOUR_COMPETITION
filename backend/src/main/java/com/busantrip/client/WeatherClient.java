package com.busantrip.client;

import org.springframework.stereotype.Component;

@Component
public class WeatherClient {

    public String getForecast(String location) {
        // 추후 WebClient로 기상청 API를 호출합니다.
        throw new UnsupportedOperationException("기상청 API 호출 예정");
    }
}

