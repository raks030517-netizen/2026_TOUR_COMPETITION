package com.roamate.backend.domain.schedule;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.domain.place.Place;
import com.roamate.backend.domain.place.PlaceRepository;
import com.roamate.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleResponse;
import com.roamate.backend.domain.user.User;
import com.roamate.backend.domain.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                            ScheduleItemRepository scheduleItemRepository,
                            UserRepository userRepository,
                            PlaceRepository placeRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.userRepository = userRepository;
        this.placeRepository = placeRepository;
    }

    @Transactional
    public ScheduleResponse createSchedule(Long userId, ScheduleCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Schedule schedule = Schedule.builder()
                .user(user)
                .title(request.title())
                .travelDate(request.travelDate())
                .build();

        scheduleRepository.save(schedule);
        return ScheduleResponse.of(schedule, List.of());
    }

    public ScheduleResponse getSchedule(Long scheduleId, Long userId) {
        Schedule schedule = findOwnedSchedule(scheduleId, userId);
        List<ScheduleItemResponse> items = scheduleItemRepository.findAllByScheduleIdOrderByVisitOrderAsc(scheduleId)
                .stream()
                .map(ScheduleItemResponse::from)
                .toList();
        return ScheduleResponse.of(schedule, items);
    }

    public List<ScheduleResponse> getMySchedules(Long userId) {
        return scheduleRepository.findAllByUserId(userId).stream()
                .map(schedule -> {
                    List<ScheduleItemResponse> items = scheduleItemRepository
                            .findAllByScheduleIdOrderByVisitOrderAsc(schedule.getId())
                            .stream()
                            .map(ScheduleItemResponse::from)
                            .toList();
                    return ScheduleResponse.of(schedule, items);
                })
                .toList();
    }

    @Transactional
    public ScheduleItemResponse addItem(Long scheduleId, Long userId, ScheduleItemCreateRequest request) {
        Schedule schedule = findOwnedSchedule(scheduleId, userId);
        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "장소를 찾을 수 없습니다."));

        ScheduleItem item = ScheduleItem.builder()
                .schedule(schedule)
                .place(place)
                .visitOrder(request.visitOrder())
                .plannedArrival(request.plannedArrival())
                .plannedDeparture(request.plannedDeparture())
                .build();

        scheduleItemRepository.save(item);
        return ScheduleItemResponse.from(item);
    }

    public Schedule findOwnedSchedule(Long scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인 일정만 접근할 수 있습니다.");
        }

        return schedule;
    }
}
