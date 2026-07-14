CREATE TABLE schedule_ai_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    success_probability INT,
    status_emoji VARCHAR(16),
    status_text VARCHAR(255),
    next_place_name VARCHAR(255),
    estimated_arrival_at TIMESTAMP,
    travel_minutes INT,
    recommended_action VARCHAR(255),
    risk_reasons VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_schedule_ai_statuses_schedule UNIQUE (schedule_id),
    CONSTRAINT fk_schedule_ai_statuses_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id)
);
