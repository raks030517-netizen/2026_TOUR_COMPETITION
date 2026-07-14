package com.roamate.backend.domain.schedule;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(@AuthenticationPrincipal Long userId,
                                                          @Valid @RequestBody ScheduleCreateRequest request) {
        return ApiResponse.ok(scheduleService.createSchedule(userId, request));
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> getSchedule(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long scheduleId) {
        return ApiResponse.ok(scheduleService.getSchedule(scheduleId, userId));
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getMySchedules(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(scheduleService.getMySchedules(userId));
    }

    @PostMapping("/{scheduleId}/items")
    public ApiResponse<ScheduleItemResponse> addItem(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long scheduleId,
                                                       @Valid @RequestBody ScheduleItemCreateRequest request) {
        return ApiResponse.ok(scheduleService.addItem(scheduleId, userId, request));
    }
}
