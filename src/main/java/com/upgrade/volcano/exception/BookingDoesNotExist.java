package com.upgrade.volcano.exception;

import java.util.UUID;

public class BookingDoesNotExist extends BaseException {


    public BookingDoesNotExist(UUID bookingId) {
        super(
                "Booking with id %s does not exist".formatted(bookingId),
                ErrorCode.BOOKING_DOES_NOT_EXIST
        );
    }
}
