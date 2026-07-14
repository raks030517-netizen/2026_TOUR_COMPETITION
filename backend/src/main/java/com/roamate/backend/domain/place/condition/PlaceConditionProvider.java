package com.roamate.backend.domain.place.condition;

import com.roamate.backend.domain.place.Place;

/**
 * BE-1이 구현할 포트. 관광공사 혼잡도 + 부산 교통 API + 기상특보 API를 조회·정규화해서
 * 이 인터페이스의 @Service 구현체를 추가하면 된다. 단 UnknownPlaceConditionProvider(스텁)를
 * 같이 두면 빈이 2개가 돼 기동이 실패하므로, 실제 구현체를 추가할 때 그 파일을 제거할 것.
 */
public interface PlaceConditionProvider {

    PlaceCondition getCondition(Place place);
}
