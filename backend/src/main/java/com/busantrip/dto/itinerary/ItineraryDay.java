package com.busantrip.dto.itinerary;

import java.time.LocalDate;
import java.util.List;

public record ItineraryDay(
        int day,
        LocalDate date,
        String headline,
        List<ItineraryStop> stops
) {
}
