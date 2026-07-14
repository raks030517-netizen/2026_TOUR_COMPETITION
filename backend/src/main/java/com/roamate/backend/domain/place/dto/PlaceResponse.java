package com.roamate.backend.domain.place.dto;

import com.roamate.backend.domain.place.Place;

public record PlaceResponse(Long id, String contentId, String name, String category,
                             String address, Double latitude, Double longitude) {

    public static PlaceResponse from(Place place) {
        return new PlaceResponse(place.getId(), place.getContentId(), place.getName(),
                place.getCategory(), place.getAddress(), place.getLatitude(), place.getLongitude());
    }
}
