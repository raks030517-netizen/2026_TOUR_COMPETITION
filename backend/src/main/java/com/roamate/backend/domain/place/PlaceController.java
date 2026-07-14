package com.roamate.backend.domain.place;

import com.roamate.backend.common.ApiResponse;
import com.roamate.backend.domain.place.dto.PlaceCreateRequest;
import com.roamate.backend.domain.place.dto.PlaceResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ApiResponse<List<PlaceResponse>> getAll() {
        return ApiResponse.ok(placeService.getAll());
    }
}
