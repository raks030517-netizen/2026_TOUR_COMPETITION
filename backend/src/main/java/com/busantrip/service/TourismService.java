package com.busantrip.service;

import com.busantrip.client.LocalTourismClient;
import com.busantrip.client.RelatedTourismClient;
import com.busantrip.dto.external.tourism.LocalTourismApiResponse;
import com.busantrip.dto.external.tourism.RelatedTourismApiResponse;
import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.dto.response.TourismResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TourismService {

    private final LocalTourismClient localTourismClient;
    private final RelatedTourismClient relatedTourismClient;

    public TourismService(LocalTourismClient localTourismClient, RelatedTourismClient relatedTourismClient) {
        this.localTourismClient = localTourismClient;
        this.relatedTourismClient = relatedTourismClient;
    }

    public Mono<TourismResponse> getLocalTourism(
            String baseYm, String signguCd, int pageNo, int numOfRows
    ) {
        return localTourismClient.getLocalTourism(baseYm, signguCd, pageNo, numOfRows)
                .map(this::toLocalResponse);
    }

    public Mono<TourismResponse> getRelatedTourismByArea(
            String baseYm, String signguCd, int pageNo, int numOfRows
    ) {
        return relatedTourismClient.getRelatedTourismByArea(baseYm, signguCd, pageNo, numOfRows)
                .map(this::toRelatedResponse);
    }

    public Mono<TourismResponse> searchRelatedTourism(
            String baseYm, String signguCd, String keyword, int pageNo, int numOfRows
    ) {
        return relatedTourismClient.searchRelatedTourism(baseYm, signguCd, keyword, pageNo, numOfRows)
                .map(this::toRelatedResponse);
    }

    private TourismResponse toLocalResponse(LocalTourismApiResponse apiResponse) {
        LocalTourismApiResponse.Body body = requireBody(apiResponse);
        List<LocalTourismApiResponse.Item> items = body.items() == null || body.items().item() == null
                ? List.of() : body.items().item();
        List<PlaceResponse> places = items.stream()
                .map(item -> new PlaceResponse(
                        item.hubTatsNm(), joinCategory(item.hubCtgryLclsNm(), item.hubCtgryMclsNm()),
                        item.signguNm(), "", parseDouble(item.mapY()), parseDouble(item.mapX()), ""))
                .toList();
        return response(body.totalCount(), body.pageNo(), body.numOfRows(), places);
    }

    private TourismResponse toRelatedResponse(RelatedTourismApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.response() == null || apiResponse.response().body() == null) {
            throw new IllegalStateException("관광지 API 응답 형식을 확인할 수 없습니다.");
        }
        RelatedTourismApiResponse.Body body = apiResponse.response().body();
        List<RelatedTourismApiResponse.Item> items = body.items() == null || body.items().item() == null
                ? List.of() : body.items().item();
        List<PlaceResponse> places = items.stream()
                .map(item -> new PlaceResponse(
                        item.rlteTatsNm(), joinCategory(item.rlteCtgryLclsNm(), item.rlteCtgryMclsNm()),
                        item.rlteSignguNm(), "", null, null, ""))
                .toList();
        return response(body.totalCount(), body.pageNo(), body.numOfRows(), places);
    }

    private LocalTourismApiResponse.Body requireBody(LocalTourismApiResponse response) {
        if (response == null || response.response() == null || response.response().body() == null) {
            throw new IllegalStateException("관광지 API 응답 형식을 확인할 수 없습니다.");
        }
        return response.response().body();
    }

    private TourismResponse response(Integer total, Integer page, Integer rows, List<PlaceResponse> places) {
        return new TourismResponse(total == null ? places.size() : total,
                page == null ? 1 : page, rows == null ? places.size() : rows, places);
    }

    private String joinCategory(String first, String second) {
        if (first == null || first.isBlank()) return second == null ? "관광지" : second;
        if (second == null || second.isBlank()) return first;
        return first + ">" + second;
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
