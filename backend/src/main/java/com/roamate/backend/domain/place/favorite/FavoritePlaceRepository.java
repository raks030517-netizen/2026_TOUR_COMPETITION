package com.roamate.backend.domain.place.favorite;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritePlaceRepository extends JpaRepository<FavoritePlace, Long> {

    Optional<FavoritePlace> findByUserIdAndPlaceId(Long userId, Long placeId);

    Page<FavoritePlace> findAllByUserId(Long userId, Pageable pageable);
}
