package com.upgrade.volcano.dto;

import com.upgrade.volcano.model.Booking;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvailabilityResponse {
    /**
     * Map date -> boolean. Date means exact date and boolean answers the question is it available?
     */
    private final Map<LocalDate, Boolean> availability;

    public AvailabilityResponse(List<Booking> bookedDates, LocalDate startDate, LocalDate endDate) {
        // populating result
        availability = new HashMap<>();
        startDate.datesUntil(endDate.plusDays(1)).forEach(d -> availability.put(d, true));

        bookedDates.forEach(d -> availability.put(d.getDate(), false));
    }

    public Map<LocalDate, Boolean> getAvailability() {
        return availability;
    }
}
