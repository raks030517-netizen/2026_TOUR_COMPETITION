package com.busantrip.service;

import com.busantrip.client.NaverLocalClient;
import com.busantrip.dto.place.NaverLocalApiResponse;
import com.busantrip.dto.response.NaverPlaceResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;

@Service
public class NaverLocalSearchService {

    private static final double NAVER_COORDINATE_SCALE = 10_000_000d;

    private final NaverLocalClient naverLocalClient;

    public NaverLocalSearchService(NaverLocalClient naverLocalClient) {
        this.naverLocalClient = naverLocalClient;
    }

    public Mono<List<NaverPlaceResponse>> search(String query) {
        return naverLocalClient.search(query)
                .map(response -> response.items().stream()
                        .map(this::toPlaceResponse)
                        .toList());
    }

    private NaverPlaceResponse toPlaceResponse(NaverLocalApiResponse.Item item) {
        return new NaverPlaceResponse(
                stripHtml(item.title()),
                item.category(),
                item.address(),
                item.roadAddress(),
                toCoordinate(item.mapy()),
                toCoordinate(item.mapx()),
                item.link()
        );
    }

    private String stripHtml(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", ""));
    }

    private double toCoordinate(long coordinate) {
        return coordinate / NAVER_COORDINATE_SCALE;
    }
}
