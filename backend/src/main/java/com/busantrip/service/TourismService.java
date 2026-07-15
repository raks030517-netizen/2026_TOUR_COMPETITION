package com.busantrip.service;

import com.busantrip.client.LocalTourismClient;
import com.busantrip.client.RelatedTourismClient;
import com.busantrip.dto.external.tourism.LocalTourismApiResponse;
import com.busantrip.dto.external.tourism.RelatedTourismApiResponse;
import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.dto.response.TourismResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TourismService {

    private final LocalTourismClient localTourismClient;
    private final RelatedTourismClient relatedTourismClient;

    public TourismService(
            LocalTourismClient localTourismClient,
            RelatedTourismClient relatedTourismClient
    ) {
        this.localTourismClient = localTourismClient;
        this.relatedTourismClient = relatedTourismClient;
    }

    /**
     * String → Double 변환
     */
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * String → Integer 변환
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 부산 기초지자체 중심 관광지 조회
     */
    public Mono<TourismResponse> getLocalTourism(
            String baseYm,
            String signguCd,
            int pageNo,
            int numOfRows
    ) {

        return localTourismClient.getLocalTourism(
                baseYm,
                signguCd,
                pageNo,
                numOfRows
        ).map(apiResponse -> {

            LocalTourismApiResponse.Body body =
                    apiResponse.response().body();

            // 공공데이터포털 API는 결과 0건일 때 items 자체가 없는 경우가 있다(빈 문자열 -> null로 역직렬화됨).
            List<LocalTourismApiResponse.Item> items =
                    body.items() == null || body.items().item() == null
                            ? List.of()
                            : body.items().item();

            List<PlaceResponse> places = items
                    .stream()
                    .map(item -> new PlaceResponse(
                            item.hubTatsNm(),
                            item.signguNm(),
                            item.hubCtgryLclsNm(),
                            item.hubCtgryMclsNm(),
                            parseDouble(item.mapY()),
                            parseDouble(item.mapX()),
                            parseInteger(item.hubRank())
                    ))
                    .toList();

            return new TourismResponse(
                    body.totalCount(),
                    body.pageNo(),
                    body.numOfRows(),
                    places
            );
        });
    }

    /**
     * 연관 관광지 조회
     */
    public Mono<TourismResponse> getRelatedTourismByArea(
            String baseYm,
            String signguCd,
            int pageNo,
            int numOfRows
    ) {

        return relatedTourismClient.getRelatedTourismByArea(
                baseYm,
                signguCd,
                pageNo,
                numOfRows
        ).map(this::toRelatedTourismResponse);

    }

    /**
     * 관광지 검색
     */
    public Mono<TourismResponse> searchRelatedTourism(
            String baseYm,
            String signguCd,
            String keyword,
            int pageNo,
            int numOfRows
    ) {

        return relatedTourismClient.searchRelatedTourism(
                baseYm,
                signguCd,
                keyword,
                pageNo,
                numOfRows
        ).map(this::toRelatedTourismResponse);

    }

    /**
     * 연관 관광지 API → TourismResponse 변환
     */
    private TourismResponse toRelatedTourismResponse(
            RelatedTourismApiResponse apiResponse
    ) {

        RelatedTourismApiResponse.Body body =
                apiResponse.response().body();

        // 공공데이터포털 API는 결과 0건일 때 items 자체가 없는 경우가 있다(빈 문자열 -> null로 역직렬화됨).
        List<RelatedTourismApiResponse.Item> items =
                body.items() == null || body.items().item() == null
                        ? List.of()
                        : body.items().item();

        List<PlaceResponse> places = items
                .stream()
                .map(item -> new PlaceResponse(

                        item.rlteTatsNm(),
                        item.rlteSignguNm(),
                        item.rlteCtgryLclsNm(),
                        item.rlteCtgryMclsNm(),

                        null,
                        null,

                        parseInteger(item.rlteRank())

                ))
                .toList();

        return new TourismResponse(
                body.totalCount(),
                body.pageNo(),
                body.numOfRows(),
                places
        );

    }

}