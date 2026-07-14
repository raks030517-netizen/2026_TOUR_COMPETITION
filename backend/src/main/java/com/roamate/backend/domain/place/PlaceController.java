package com.roamate.backend.domain.place;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.common.PageResponse;
import com.roamate.backend.domain.place.dto.PlaceConditionResponse;
import com.roamate.backend.domain.place.dto.PlaceCreateRequest;
import com.roamate.backend.domain.place.dto.PlaceResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @PostMapping
    public ApiResponse<PlaceResponse> register(@Valid @RequestBody PlaceCreateRequest request) {
        return ApiResponse.ok(placeService.register(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<PlaceResponse>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(placeService.getAll(pageable));
    }

    @GetMapping("/{placeId}/condition")
    public ApiResponse<PlaceConditionResponse> getCondition(@PathVariable Long placeId) {
        return ApiResponse.ok(placeService.getCondition(placeId));
    }
}
