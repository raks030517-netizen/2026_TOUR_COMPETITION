package com.roamate.backend.domain.action.dto;

import com.roamate.backend.domain.action.ActionType;

public record ActionResponse(
        ActionType actionType,
        String message
) {
}
