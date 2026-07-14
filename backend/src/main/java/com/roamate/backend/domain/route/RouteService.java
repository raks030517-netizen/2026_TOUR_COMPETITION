package com.roamate.backend.domain.route;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.domain.place.Place;
import com.roamate.backend.domain.place.PlaceRepository;
import com.roamate.backend.domain.route.dto.RouteOptimizeRequest;
import com.roamate.backend.domain.route.dto.RouteStopResponse;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final PlaceRepository placeRepository;
    private final SmartRouteService smartRouteService;

    public RouteService(PlaceRepository placeRepository, SmartRouteService smartRouteService) {
        this.placeRepository = placeRepository;
        this.smartRouteService = smartRouteService;
    }

    public List<RouteStopResponse> optimize(RouteOptimizeRequest request) {
        List<Place> places = placeRepository.findAllById(request.placeIds());
        if (places.size() != request.placeIds().size()) {
            throw new ApiException(ErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 장소가 포함되어 있습니다.");
        }

        List<Place> ordered = smartRouteService.optimizeOrder(places, request.startLatitude(), request.startLongitude());

        return IntStream.range(0, ordered.size())
                .mapToObj(i -> RouteStopResponse.of(i + 1, ordered.get(i)))
                .toList();
    }
}
