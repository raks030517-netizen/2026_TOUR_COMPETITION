package com.busantrip.dto.travel;

import com.busantrip.dto.llm.SearchCondition;
import java.util.List;

public record TravelSearchResponse(
        String message,
        SearchCondition condition,
        List<TravelPlaceResponse> places,
        List<PartialFailure> partialFailures
) {
}
