package com.busantrip.dto.place;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverLocalApiResponse(List<Item> items) {

    public NaverLocalApiResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static NaverLocalApiResponse empty() {
        return new NaverLocalApiResponse(List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String title,
            String link,
            String category,
            String address,
            String roadAddress,
            long mapx,
            long mapy
    ) {
    }
}
