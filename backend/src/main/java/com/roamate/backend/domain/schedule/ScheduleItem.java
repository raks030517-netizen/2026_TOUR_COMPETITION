package com.roamate.backend.domain.schedule;

import com.roamate.backend.domain.BaseTimeEntity;
import com.roamate.backend.domain.place.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule_items")
public class ScheduleItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder;

    @Column(name = "planned_arrival")
    private LocalDateTime plannedArrival;

    @Column(name = "planned_departure")
    private LocalDateTime plannedDeparture;

    @Builder
    public ScheduleItem(Schedule schedule, Place place, int visitOrder,
                         LocalDateTime plannedArrival, LocalDateTime plannedDeparture) {
        this.schedule = schedule;
        this.place = place;
        this.visitOrder = visitOrder;
        this.plannedArrival = plannedArrival;
        this.plannedDeparture = plannedDeparture;
    }

    public void reorder(int visitOrder) {
        this.visitOrder = visitOrder;
    }

    public void updatePlannedTimes(LocalDateTime plannedArrival, LocalDateTime plannedDeparture) {
        this.plannedArrival = plannedArrival;
        this.plannedDeparture = plannedDeparture;
    }
}
