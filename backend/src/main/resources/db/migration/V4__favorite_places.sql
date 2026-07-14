CREATE TABLE favorite_places (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_favorite_places_user_place UNIQUE (user_id, place_id),
    CONSTRAINT fk_favorite_places_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_favorite_places_place FOREIGN KEY (place_id) REFERENCES places (id)
);
