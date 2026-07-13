package com.busantrip.dto.response;

import java.util.List;

public record ChatResponse(String message, List<PlaceResponse> places) {
}

