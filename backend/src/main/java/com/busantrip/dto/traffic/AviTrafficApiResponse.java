package com.busantrip.dto.traffic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AviTrafficApiResponse(
        String resultCode,
        String resultMsg,
        Content content
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            Integer pageNo,
            Integer numOfRows,
            Integer totalCount,
            JsonNode items
    ) {
    }
}
