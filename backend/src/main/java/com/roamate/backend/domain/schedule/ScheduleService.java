package com.roamate.backend.domain.schedule;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.common.PageResponse;
import com.roamate.backend.domain.place.Place;
import com.roamate.backend.domain.place.PlaceRepository;
import com.roamate.backend.domain.schedule.dto.ReorderRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleAiStatusResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemCreateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleItemResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleItemUpdateRequest;
import com.roamate.backend.domain.schedule.dto.ScheduleResponse;
import com.roamate.backend.domain.schedule.dto.ScheduleUpdateRequest;
import com.roamate.backend.domain.user.User;
import com.roamate.backend.domain.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleAiStatusRepository scheduleAiStatusRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                            ScheduleItemRepository scheduleItemRepository,
                            ScheduleAiStatusRepository scheduleAiStatusRepository,
                            UserRepository userRepository,
                            PlaceRepository placeRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleAiStatusRepository = scheduleAiStatusRepository;
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
        return ScheduleResponse.of(schedule, List.of(), null);
    }

    public ScheduleResponse getSchedule(Long scheduleId, Long userId) {
        Schedule schedule = findOwnedSchedule(scheduleId, userId);
        List<ScheduleItemResponse> items = scheduleItemRepository.findAllByScheduleIdOrderByVisitOrderAsc(scheduleId)
                .stream()
                .map(ScheduleItemResponse::from)
                .toList();
        ScheduleAiStatusResponse aiStatus = scheduleAiStatusRepository.findByScheduleId(scheduleId)
                .map(ScheduleAiStatusResponse::from)
                .orElse(null);
        return ScheduleResponse.of(schedule, items, aiStatus);
    }

    public PageResponse<ScheduleResponse> getMySchedules(Long userId, Pageable pageable) {
        Page<ScheduleResponse> page = scheduleRepository.findAllByUserId(userId, pageable)
                .map(schedule -> {
                    List<ScheduleItemResponse> items = scheduleItemRepository
                            .findAllByScheduleIdOrderByVisitOrderAsc(schedule.getId())
                            .stream()
                            .map(ScheduleItemResponse::from)
                            .toList();
                    ScheduleAiStatusResponse aiStatus = scheduleAiStatusRepository.findByScheduleId(schedule.getId())
                            .map(ScheduleAiStatusResponse::from)
                            .orElse(null);
                    return ScheduleResponse.of(schedule, items, aiStatus);
                });
        return PageResponse.of(page);
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

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, Long userId, ScheduleUpdateRequest request) {
        Schedule schedule = findOwnedSchedule(scheduleId, userId);
        schedule.update(request.title(), request.travelDate());
        return getSchedule(scheduleId, userId);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        Schedule schedule = findOwnedSchedule(scheduleId, userId);
        scheduleItemRepository.deleteAll(scheduleItemRepository.findAllByScheduleIdOrderByVisitOrderAsc(scheduleId));
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public ScheduleItemResponse updateItem(Long scheduleId, Long itemId, Long userId, ScheduleItemUpdateRequest request) {
        findOwnedSchedule(scheduleId, userId);
        ScheduleItem item = findOwnedItem(scheduleId, itemId);
        item.updatePlannedTimes(request.plannedArrival(), request.plannedDeparture());
        return ScheduleItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long scheduleId, Long itemId, Long userId) {
        findOwnedSchedule(scheduleId, userId);
        ScheduleItem item = findOwnedItem(scheduleId, itemId);
        scheduleItemRepository.delete(item);
    }

    @Transactional
    public List<ScheduleItemResponse> reorderItems(Long scheduleId, Long userId, ReorderRequest request) {
        findOwnedSchedule(scheduleId, userId);
        List<ScheduleItem> items = scheduleItemRepository.findAllByScheduleIdOrderByVisitOrderAsc(scheduleId);
        Map<Long, ScheduleItem> itemsById = items.stream()
                .collect(Collectors.toMap(ScheduleItem::getId, item -> item));

        if (request.itemIds().size() != items.size() || !itemsById.keySet().containsAll(request.itemIds())) {
            throw new ApiException(ErrorCode.INVALID_INPUT, "일정의 항목 목록과 일치하지 않습니다.");
        }

        List<Long> orderedIds = request.itemIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            itemsById.get(orderedIds.get(i)).reorder(i + 1);
        }

        return orderedIds.stream()
                .map(id -> ScheduleItemResponse.from(itemsById.get(id)))
                .toList();
    }

    private ScheduleItem findOwnedItem(Long scheduleId, Long itemId) {
        return scheduleItemRepository.findByIdAndScheduleId(itemId, scheduleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "일정 항목을 찾을 수 없습니다."));
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
