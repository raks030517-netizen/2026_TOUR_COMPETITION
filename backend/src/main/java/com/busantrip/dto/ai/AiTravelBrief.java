package com.busantrip.dto.ai;

import java.util.List;

public record AiTravelBrief(
        String title,
        String summary,
        String recommendedArea,
        List<String> activities,
        List<String> cautions
) {
}
