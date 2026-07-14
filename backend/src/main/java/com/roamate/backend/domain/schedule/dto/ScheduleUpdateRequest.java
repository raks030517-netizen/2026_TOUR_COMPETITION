package com.roamate.backend.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleUpdateRequest(
        @NotBlank String title,
        @NotNull LocalDate travelDate
) {
}
