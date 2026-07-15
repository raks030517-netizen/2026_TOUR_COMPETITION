package com.busantrip.service;

import com.busantrip.client.LocalTourismClient;
import com.busantrip.client.RelatedTourismClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
     * 부산 기초지자체 중심 관광지 조회
     */
    public Mono<String> getLocalTourism(
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
        );
    }

    /**
     * 특정 지역의 연관 관광지 조회
     */
    public Mono<String> getRelatedTourismByArea(
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
        );
    }

    /**
     * 관광지 이름으로 연관 관광지 검색
     */
    public Mono<String> searchRelatedTourism(
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
        );
    }
}