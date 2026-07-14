package com.roamate.backend.domain.action;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.domain.action.dto.ActionRequest;
import com.roamate.backend.domain.action.dto.ActionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
public class OneTouchActionController {

    private final OneTouchActionService oneTouchActionService;

    public OneTouchActionController(OneTouchActionService oneTouchActionService) {
        this.oneTouchActionService = oneTouchActionService;
    }

    @PostMapping("/{actionType}")
    public ApiResponse<ActionResponse> execute(@PathVariable ActionType actionType,
                                                 @Valid @RequestBody ActionRequest request) {
        return ApiResponse.ok(oneTouchActionService.execute(actionType, request.scheduleId()));
    }
}
