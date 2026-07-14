package com.roamate.backend.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleCreateRequest(
        @NotNull Long userId,
        @NotBlank String title,
        @NotNull LocalDate travelDate
) {
}
