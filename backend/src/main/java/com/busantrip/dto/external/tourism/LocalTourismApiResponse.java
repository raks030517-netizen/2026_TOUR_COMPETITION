package com.busantrip.dto.external.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 한국관광공사 기초지자체 중심 관광지 API의 원본 응답 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalTourismApiResponse(
        Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Header header,
            Body body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            Items items,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            List<Item> item
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String baseYm,
            String mapX,
            String mapY,
            String areaCd,
            String areaNm,
            String signguCd,
            String signguNm,
            String hubTatsCd,
            String hubTatsNm,
            String hubCtgryLclsNm,
            String hubCtgryMclsNm,
            String hubRank
    ) {
    }
}
