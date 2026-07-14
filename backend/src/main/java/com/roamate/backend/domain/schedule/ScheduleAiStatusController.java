package com.roamate.backend.domain.schedule;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleAiStatusResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleAiStatusUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BE-2(AI 서비스)가 계산 결과를 갱신하는 용도 — 사용자 JWT가 아니라 내부 호출을 전제로
 * SecurityConfig에서 permitAll 처리돼 있다. 서비스 간 인증은 별도 결정 필요(추후).
 */
@RestController
@RequestMapping("/api/schedules/{scheduleId}/ai-status")
public class ScheduleAiStatusController {

    private final ScheduleAiStatusService scheduleAiStatusService;

    public ScheduleAiStatusController(ScheduleAiStatusService scheduleAiStatusService) {
        this.scheduleAiStatusService = scheduleAiStatusService;
    }

    @PutMapping
    public ApiResponse<ScheduleAiStatusResponse> upsert(@PathVariable Long scheduleId,
                                                          @Valid @RequestBody ScheduleAiStatusUpdateRequest request) {
        return ApiResponse.ok(scheduleAiStatusService.upsert(scheduleId, request));
    }
}
