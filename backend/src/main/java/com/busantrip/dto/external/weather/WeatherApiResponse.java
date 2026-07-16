package com.busantrip.dto.external.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 기상청 단기예보 API 원본 응답 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherApiResponse(
        Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Header header,
            Body body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            Items items,
            Integer pageNo,
            Integer numOfRows,
            Integer totalCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            List<Item> item
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(

            String baseDate,
            String baseTime,

            String category,

            String fcstDate,
            String fcstTime,

            String fcstValue,

            Integer nx,
            Integer ny

    ) {}
}
