package com.roamate.backend.domain.schedule;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.domain.schedule.dto.ScheduleAiStatusResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleAiStatusUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduleAiStatusService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleAiStatusRepository scheduleAiStatusRepository;

    public ScheduleAiStatusService(ScheduleRepository scheduleRepository,
                                    ScheduleAiStatusRepository scheduleAiStatusRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleAiStatusRepository = scheduleAiStatusRepository;
    }

    @Transactional
    public ScheduleAiStatusResponse upsert(Long scheduleId, ScheduleAiStatusUpdateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));

        ScheduleAiStatus status = scheduleAiStatusRepository.findByScheduleId(scheduleId)
                .orElseGet(() -> ScheduleAiStatus.builder().schedule(schedule).build());

        status.update(request.successProbability(), request.statusEmoji(), request.statusText(),
                request.nextPlaceName(), request.estimatedArrivalAt(), request.travelMinutes(),
                request.recommendedAction(), request.riskReasons());

        scheduleAiStatusRepository.save(status);
        return ScheduleAiStatusResponse.from(status);
    }
}
