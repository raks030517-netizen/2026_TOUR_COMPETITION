package com.busantrip.dto.response;

import java.util.List;

public record RelatedTourismResponse(
        int totalCount,
        int pageNo,
        int numOfRows,
        List<PlaceResponse> places
) {
}