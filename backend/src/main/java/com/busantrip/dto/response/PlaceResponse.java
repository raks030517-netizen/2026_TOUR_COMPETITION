package com.busantrip.dto.response;

/**
 * 화면과 Spring AI에 전달할 관광지 요약 정보.
 */
public record PlaceResponse(
        String name,
        String district,
        String category,
        String subCategory,
        Double latitude,
        Double longitude,
        Integer rank
) {
}