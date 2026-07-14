package com.roamate.backend.domain.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    List<ScheduleItem> findAllByScheduleIdOrderByVisitOrderAsc(Long scheduleId);
}
