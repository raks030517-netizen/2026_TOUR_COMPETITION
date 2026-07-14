package com.roamate.backend.domain.place.favorite;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.common.PageResponse;
import com.roamate.backend.domain.place.dto.PlaceResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class FavoritePlaceController {

    private final FavoritePlaceService favoritePlaceService;

    public FavoritePlaceController(FavoritePlaceService favoritePlaceService) {
        this.favoritePlaceService = favoritePlaceService;
    }

    @PostMapping("/{placeId}/favorite")
    public ApiResponse<Void> addFavorite(@AuthenticationPrincipal Long userId, @PathVariable Long placeId) {
        favoritePlaceService.addFavorite(userId, placeId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{placeId}/favorite")
    public ApiResponse<Void> removeFavorite(@AuthenticationPrincipal Long userId, @PathVariable Long placeId) {
        favoritePlaceService.removeFavorite(userId, placeId);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<PlaceResponse>> getMyFavorites(@AuthenticationPrincipal Long userId,
                                                                     @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(favoritePlaceService.getMyFavorites(userId, pageable));
    }
}
