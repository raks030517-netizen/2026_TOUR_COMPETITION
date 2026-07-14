package com.roamate.backend.domain.schedule;

import com.roamate.backend.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * README의 "일정 위험도 예측"·"AI 여행 카드" 기능이 쓸 계산 결과 저장소.
 * 계산 로직(성공확률·추천행동 산출)은 BE-2 담당 — 이 엔티티는 그 결과를 담는 스키마다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule_ai_statuses")
public class ScheduleAiStatus extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false, unique = true)
    private Schedule schedule;

    @Column(name = "success_probability")
    private Integer successProbability;

    @Column(name = "status_emoji")
    private String statusEmoji;

    @Column(name = "status_text")
    private String statusText;

    @Column(name = "next_place_name")
    private String nextPlaceName;

    @Column(name = "estimated_arrival_at")
    private LocalDateTime estimatedArrivalAt;

    @Column(name = "travel_minutes")
    private Integer travelMinutes;

    @Column(name = "recommended_action")
    private String recommendedAction;

    @Column(name = "risk_reasons")
    private String riskReasons;

    @Builder
    public ScheduleAiStatus(Schedule schedule) {
        this.schedule = schedule;
    }

    public void update(Integer successProbability, String statusEmoji, String statusText,
                        String nextPlaceName, LocalDateTime estimatedArrivalAt, Integer travelMinutes,
                        String recommendedAction, String riskReasons) {
        this.successProbability = successProbability;
        this.statusEmoji = statusEmoji;
        this.statusText = statusText;
        this.nextPlaceName = nextPlaceName;
        this.estimatedArrivalAt = estimatedArrivalAt;
        this.travelMinutes = travelMinutes;
        this.recommendedAction = recommendedAction;
        this.riskReasons = riskReasons;
    }
}
