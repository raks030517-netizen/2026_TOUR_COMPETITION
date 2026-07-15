package com.busantrip.dto.external.tourism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 한국관광공사 연관 관광지 API 원본 응답 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelatedTourismApiResponse(
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
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(
            List<Item> item
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(

            String baseYm,

            String tAtsCd,
            String tAtsNm,

            String areaCd,
            String areaNm,

            String signguCd,
            String signguNm,

            String rlteTatsCd,
            String rlteTatsNm,

            String rlteRegnCd,
            String rlteRegnNm,

            String rlteSignguCd,
            String rlteSignguNm,

            String rlteCtgryLclsNm,
            String rlteCtgryMclsNm,

            String rlteRank

    ) {}
}