package com.busantrip.dto.itinerary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** 여행자가 입력하는 일정 설계 조건이다. 현재 발표 범위는 부산으로 제한한다. */
public record ItineraryRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty @Size(max = 4) List<@NotBlank String> themes,
        @NotBlank @Size(max = 30) String companion,
        @NotBlank @Size(max = 30) String transport,
        @NotBlank @Size(max = 30) String pace,
        @NotNull @Valid StartPoint start
) {
    public record StartPoint(
            @Min(-90) @Max(90) double latitude,
            @Min(-180) @Max(180) double longitude
    ) {
    }
}
