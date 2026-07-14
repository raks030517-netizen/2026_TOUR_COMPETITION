package com.roamate.backend.domain.place.condition;

import com.roamate.backend.domain.place.Place;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * BE-1의 실제 구현체가 아직 없을 때 쓰는 기본값 — 항상 UNKNOWN을 반환한다.
 * BE-1이 PlaceConditionProvider를 구현한 @Service를 추가할 때는 이 파일을 지우거나
 * @Service를 떼서 빈 중복(같은 인터페이스에 빈 2개) 에러가 안 나게 할 것.
 */
@Service
public class UnknownPlaceConditionProvider implements PlaceConditionProvider {

    @Override
    public PlaceCondition getCondition(Place place) {
        return PlaceCondition.unknown(LocalDateTime.now());
    }
}
