package com.busantrip.service;

import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    public String getWeather(String location) {
        // 추후 위치에 맞는 기상 정보를 조회합니다.
        throw new UnsupportedOperationException("기상청 API 연동 예정");
    }
}

