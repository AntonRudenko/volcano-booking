package com.upgrade.volcano.exception;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class BookingForDatesExist extends BaseException {
    public BookingForDatesExist(List<LocalDate> dates) {
        super(
                "There is another Booking for dates %s".formatted(
                        dates.stream().map(Object::toString).collect(Collectors.joining(","))
                ),
                ErrorCode.BOOKING_FOR_DATE_EXIST
        );
    }
}
