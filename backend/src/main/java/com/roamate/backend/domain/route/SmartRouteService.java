package com.roamate.backend.domain.route;

import com.roamate.backend.domain.place.Place;
import java.util.List;

/**
 * 방문 순서 최적화 인터페이스. 지금은 최근접(NearestNeighborRouteService)만 구현돼 있고,
 * BE-2가 날씨/혼잡도/AI 가중치를 반영한 구현체를 추가해 교체할 수 있는 지점이다.
 */
public interface SmartRouteService {

    List<Place> optimizeOrder(List<Place> places, Double startLatitude, Double startLongitude);
}
