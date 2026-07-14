package com.roamate.backend.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.domain.place.PlaceRepository;
import com.roamate.backend.domain.user.User;
import com.roamate.backend.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SCHEDULE_ID = 100L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleItemRepository scheduleItemRepository;

    @Mock
    private ScheduleAiStatusRepository scheduleAiStatusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaceRepository placeRepository;

    private ScheduleService scheduleService;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository, scheduleItemRepository,
                scheduleAiStatusRepository, userRepository, placeRepository);

        User owner = User.builder().email("owner@roamate.dev").password("hash").nickname("owner").build();
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);

        schedule = Schedule.builder().user(owner).title("Busan Trip").travelDate(LocalDate.of(2026, 8, 1)).build();
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
    }

    @Test
    void 본인_일정이면_정상_반환한다() {
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        Schedule found = scheduleService.findOwnedSchedule(SCHEDULE_ID, OWNER_ID);

        assertThat(found).isSameAs(schedule);
    }

    @Test
    void 존재하지_않는_일정이면_ENTITY_NOT_FOUND() {
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.findOwnedSchedule(SCHEDULE_ID, OWNER_ID))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    void 타인의_일정이면_FORBIDDEN() {
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.findOwnedSchedule(SCHEDULE_ID, OTHER_USER_ID))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
