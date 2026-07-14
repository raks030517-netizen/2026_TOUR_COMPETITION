package com.busantrip.dto.response;

public record PlaceResponse(
        String name,
        String address,
        Double latitude,
        Double longitude
) {
}

