package com.busantrip.dto.response;

import java.util.List;

/**
 * 관광지 조회 결과를 화면과 AI 서비스에 전달하기 위한 응답 DTO.
 */
public record TourismResponse(
        int totalCount,
        int pageNo,
        int numOfRows,
        List<PlaceResponse> places,
        String source
) {

    public TourismResponse(int totalCount, int pageNo, int numOfRows, List<PlaceResponse> places) {
        this(totalCount, pageNo, numOfRows, places, "live");
    }
}
