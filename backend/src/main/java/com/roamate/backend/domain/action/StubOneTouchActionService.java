package com.roamate.backend.domain.action;

import com.roamate.backend.domain.action.dto.ActionResponse;
import com.roamate.backend.domain.schedule.ScheduleService;
import org.springframework.stereotype.Service;

@Service
public class StubOneTouchActionService implements OneTouchActionService {

    private final ScheduleService scheduleService;

    public StubOneTouchActionService(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public ActionResponse execute(ActionType actionType, Long scheduleId, Long userId) {
        scheduleService.findOwnedSchedule(scheduleId, userId);
        return new ActionResponse(actionType, "AI 연동 준비 중입니다 (BE-2 작업 대기).");
    }
}
