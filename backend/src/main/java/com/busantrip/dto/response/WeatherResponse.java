package com.busantrip.dto.response;

/**
 * 화면과 Spring AI에 전달할 날씨 요약 응답 DTO.
 *  forecastDate              예보 날짜
 *  forecastTime              예보 시간
 *  temperature               기온(TMP)
 *  precipitationProbability  강수확률(POP)
 *  precipitationType         강수형태(PTY)
 *  skyCondition              하늘상태(SKY)
 *  windSpeed                 풍속(WSD)
 *  humidity                  습도(REH)
 */
public record WeatherResponse(
        String forecastDate,
        String forecastTime,
        Double temperature,
        Integer precipitationProbability,
        String precipitationType,
        String skyCondition,
        Double windSpeed,
        Integer humidity
) {
}
