package com.roamate.backend.domain.place;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.common.PageResponse;
import com.roamate.backend.domain.place.condition.PlaceConditionProvider;
import com.roamate.backend.domain.place.dto.PlaceConditionResponse;
import com.roamate.backend.domain.place.dto.PlaceCreateRequest;
import com.roamate.backend.domain.place.dto.PlaceResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관광공사 OpenAPI 연동(BE-1) 전까지 임시로 장소를 수동 등록하기 위한 서비스.
 * 실제 데이터 수집이 붙으면 이 API 대신 배치/스케줄러가 채우는 방식으로 대체될 수 있다.
 */
@Service
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceConditionProvider placeConditionProvider;

    public PlaceService(PlaceRepository placeRepository, PlaceConditionProvider placeConditionProvider) {
        this.placeRepository = placeRepository;
        this.placeConditionProvider = placeConditionProvider;
    }

    @Transactional
    public PlaceResponse register(PlaceCreateRequest request) {
        Place place = placeRepository.findByContentId(request.contentId())
                .orElseGet(() -> Place.builder()
                        .contentId(request.contentId())
                        .name(request.name())
                        .category(request.category())
                        .address(request.address())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .imageUrl(request.imageUrl())
                        .build());

        placeRepository.save(place);
        return PlaceResponse.from(place);
    }

    public PageResponse<PlaceResponse> getAll(Pageable pageable) {
        return PageResponse.of(placeRepository.findAll(pageable).map(PlaceResponse::from));
    }

    public PlaceConditionResponse getCondition(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "장소를 찾을 수 없습니다."));
        return PlaceConditionResponse.from(placeConditionProvider.getCondition(place));
    }
}
