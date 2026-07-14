package com.roamate.backend.domain.action.dto;

import jakarta.validation.constraints.NotNull;

public record ActionRequest(
        @NotNull Long scheduleId
) {
}
