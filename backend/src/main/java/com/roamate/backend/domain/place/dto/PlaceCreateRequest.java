package com.roamate.backend.domain.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceCreateRequest(
        @NotBlank String contentId,
        @NotBlank String name,
        String category,
        String address,
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
