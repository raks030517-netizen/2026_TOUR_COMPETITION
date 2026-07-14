package com.roamate.backend.domain.place.favorite;

import com.roamate.backend.common.ApiException;
import com.roamate.backend.common.ErrorCode;
import com.roamate.backend.common.PageResponse;
import com.roamate.backend.domain.place.Place;
import com.roamate.backend.domain.place.PlaceRepository;
import com.roamate.backend.domain.place.dto.PlaceResponse;
import com.roamate.backend.domain.user.User;
import com.roamate.backend.domain.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FavoritePlaceService {

    private final FavoritePlaceRepository favoritePlaceRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    public FavoritePlaceService(FavoritePlaceRepository favoritePlaceRepository, PlaceRepository placeRepository,
                                 UserRepository userRepository) {
        this.favoritePlaceRepository = favoritePlaceRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addFavorite(Long userId, Long placeId) {
        if (favoritePlaceRepository.findByUserIdAndPlaceId(userId, placeId).isPresent()) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND, "장소를 찾을 수 없습니다."));

        favoritePlaceRepository.save(FavoritePlace.builder().user(user).place(place).build());
    }

    @Transactional
    public void removeFavorite(Long userId, Long placeId) {
        favoritePlaceRepository.findByUserIdAndPlaceId(userId, placeId)
                .ifPresent(favoritePlaceRepository::delete);
    }

    public PageResponse<PlaceResponse> getMyFavorites(Long userId, Pageable pageable) {
        return PageResponse.of(favoritePlaceRepository.findAllByUserId(userId, pageable)
                .map(favorite -> PlaceResponse.from(favorite.getPlace())));
    }
}
