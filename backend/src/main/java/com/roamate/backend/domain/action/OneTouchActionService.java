package com.roamate.backend.domain.action;

import com.roamate.backend.domain.action.dto.ActionResponse;

/**
 * BE-2가 Gemma 연동 후 이 인터페이스의 실제 구현체(AI 기반)로 교체한다.
 * 지금은 StubOneTouchActionService만 존재.
 */
public interface OneTouchActionService {

    ActionResponse execute(ActionType actionType, Long scheduleId);
}
