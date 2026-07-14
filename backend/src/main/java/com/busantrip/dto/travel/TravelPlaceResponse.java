package com.busantrip.dto.travel;

public record TravelPlaceResponse(
        String name,
        PlaceType type,
        String category,
        String address,
        String roadAddress,
        Double latitude,
        Double longitude,
        String link
) {
}
