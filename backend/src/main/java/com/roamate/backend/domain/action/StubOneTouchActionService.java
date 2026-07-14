package com.roamate.backend.domain.action;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.domain.action.dto.ActionResponse;
import com.roamate.backend.domain.schedule.ScheduleRepository;
import org.springframework.stereotype.Service;

@Service
public class StubOneTouchActionService implements OneTouchActionService {

    private final ScheduleRepository scheduleRepository;

    public StubOneTouchActionService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public ActionResponse execute(ActionType actionType, Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ApiException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다.");
        }
        return new ActionResponse(actionType, "AI 연동 준비 중입니다 (BE-2 작업 대기).");
    }
}
