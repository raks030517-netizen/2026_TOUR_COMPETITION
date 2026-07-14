package com.roamate.backend.domain.schedule;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        return ApiResponse.ok(scheduleService.createSchedule(request));
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> getSchedule(@PathVariable Long scheduleId) {
        return ApiResponse.ok(scheduleService.getSchedule(scheduleId));
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getSchedulesByUser(@RequestParam Long userId) {
        return ApiResponse.ok(scheduleService.getSchedulesByUser(userId));
    }

    @PostMapping("/{scheduleId}/items")
    public ApiResponse<ScheduleItemResponse> addItem(@PathVariable Long scheduleId,
                                                       @Valid @RequestBody ScheduleItemCreateRequest request) {
        return ApiResponse.ok(scheduleService.addItem(scheduleId, request));
    }
}
