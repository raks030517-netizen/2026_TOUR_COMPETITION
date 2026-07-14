package com.roamate.backend.domain.schedule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    List<ScheduleItem> findAllByScheduleIdOrderByVisitOrderAsc(Long scheduleId);

    Optional<ScheduleItem> findByIdAndScheduleId(Long id, Long scheduleId);
}
