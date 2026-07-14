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
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Schedule schedule = Schedule.builder()
                .user(user)
                .title(request.title())
                .travelDate(request.travelDate())
                .build();

        scheduleRepository.save(schedule);
        return ScheduleResponse.of(schedule, List.of());
    }

    public ScheduleResponse getSchedule(Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        List<ScheduleItemResponse> items = scheduleItemRepository.findAllByScheduleIdOrderByVisitOrderAsc(scheduleId)
                .stream()
                .map(ScheduleItemResponse::from)
                .toList();
        return ScheduleResponse.of(schedule, items);
    }

    public List<ScheduleResponse> getSchedulesByUser(Long userId) {
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
    public ScheduleItemResponse addItem(Long scheduleId, ScheduleItemCreateRequest request) {
        Schedule schedule = findSchedule(scheduleId);
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

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "일정을 찾을 수 없습니다."));
    }
}
