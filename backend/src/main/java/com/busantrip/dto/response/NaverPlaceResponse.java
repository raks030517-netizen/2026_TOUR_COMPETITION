package com.busantrip.dto.response;

/**
 * 네이버 지역 검색 API 원본 결과를 그대로 담는 DTO.
 * (관광공사 API 기반 {@link PlaceResponse}와는 출처·필드가 달라 이름을 분리했다.)
 */
public record NaverPlaceResponse(
        String name,
        String category,
        String address,
        String roadAddress,
        Double latitude,
        Double longitude,
        String link
) {
}
