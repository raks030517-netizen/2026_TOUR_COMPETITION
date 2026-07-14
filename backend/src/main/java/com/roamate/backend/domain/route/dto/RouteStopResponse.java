package com.roamate.backend.domain.route.dto;

import com.roamate.backend.domain.place.Place;

public record RouteStopResponse(
        int order,
        Long placeId,
        String placeName,
        Double latitude,
        Double longitude
) {
    public static RouteStopResponse of(int order, Place place) {
        return new RouteStopResponse(order, place.getId(), place.getName(), place.getLatitude(), place.getLongitude());
    }
}
