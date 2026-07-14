package com.roamate.backend.domain.schedule;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleAiStatusRepository extends JpaRepository<ScheduleAiStatus, Long> {

    Optional<ScheduleAiStatus> findByScheduleId(Long scheduleId);
}
