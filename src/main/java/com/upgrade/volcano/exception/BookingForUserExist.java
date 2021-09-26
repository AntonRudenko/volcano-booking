package com.upgrade.volcano.exception;

public class BookingForUserExist extends BaseException {

    public BookingForUserExist(String email) {
        super(
                "Booking for user %s already exists".formatted(email),
                ErrorCode.BOOKING_FOR_USER_EXIST
        );
    }
}
