package com.upgrade.volcano.controller;

import com.upgrade.volcano.dto.AvailabilityResponse;
import com.upgrade.volcano.dto.BookingRequest;
import com.upgrade.volcano.dto.BookingResponse;
import com.upgrade.volcano.dto.UpdateBookingRequest;
import com.upgrade.volcano.exception.BookingForDatesExist;
import com.upgrade.volcano.exception.ClientValidationException;
import com.upgrade.volcano.model.Booking;
import com.upgrade.volcano.service.BookingService;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("booking")
public class BookingController {

    private final BookingService bookingService;

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);
    public static final String UNIQUE_BOOKING_DATE_CONSTRAINT_NAME = "unique_booking_date";

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Cacheable("availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate == null && endDate == null) {
            startDate = LocalDate.now();
            endDate = startDate.plusMonths(1);
        }

        if (oneDayIsNullAndOtherIsNot(startDate, endDate)) {
            throw new ClientValidationException("Start date and end date must be set together");
        }

        if (startDate.isAfter(endDate)) {
            throw new ClientValidationException("Start date is later than end date");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new ClientValidationException("Start date is in the past");
        }

        List<Booking> bookedDates = bookingService.getBookedDates(startDate, endDate);
        return ResponseEntity.ok(new AvailabilityResponse(bookedDates, startDate, endDate));
    }

    private boolean oneDayIsNullAndOtherIsNot(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate startDate, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate endDate) {
        return (startDate == null && endDate != null) || (startDate != null && endDate == null);
    }

    @PostMapping
    @CacheEvict(value = "availability", allEntries = true)
    public ResponseEntity<BookingResponse> book(@RequestBody @Valid BookingRequest r) {
        validateBookingRequest(r);

        var bookingId = saveBookingWithErrorHandling(r);
        log.info("Booking for {} and dates {} - {} was created successfully", r.getEmail(), r.getStartDate(), r.getEndDate());

        return ResponseEntity.ok(new BookingResponse(bookingId));
    }

    // right now booking request and update request are the same, so I will use it, but if they will differ in the
    // future it will be easy change
    @PutMapping("/{id}")
    @CacheEvict(value = "availability", allEntries = true)
    public ResponseEntity<Void> updateBooking(@PathVariable String id, @RequestBody @Valid UpdateBookingRequest r) {
        UUID bookingId = fromString(id);
        validateUpdateRequest(r);

        bookingService.update(bookingId, r.getStartDate(), r.getEndDate(), r.getEmail(), r.getName());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "availability", allEntries = true)
    public ResponseEntity<Void> cancelBooking(@PathVariable String id) {
        UUID bookingUUID = fromString(id);

        bookingService.cancel(bookingUUID);
        return ResponseEntity.ok().build();
    }

    // this is our additional protection against multithreading booking requests
    private UUID saveBookingWithErrorHandling(BookingRequest r) {
        try {
            return bookingService.book(r.getStartDate(), r.getEndDate(), r.getEmail(), r.getName());
        } catch (DataIntegrityViolationException e) {
            if (isSameDateBookingException(e)) {
                throw new BookingForDatesExist(r.getStartDate().datesUntil(r.getEndDate()).collect(Collectors.toList()));
            } else {
                // error is unexpected we throw as is
                throw e;
            }
        }
    }

    private boolean isSameDateBookingException(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        if (cause == null) {
            return false;
        }

        if (!(cause instanceof ConstraintViolationException constraintException)) {
            return false;
        }

        String constraintName = constraintException.getConstraintName();
        if (!StringUtils.hasText(constraintName)) {
            return false;
        }

        return constraintName.toLowerCase().contains(UNIQUE_BOOKING_DATE_CONSTRAINT_NAME);
    }

    private void validateBookingRequest(BookingRequest r) {
        validateEndDateLaterStartDate(r.getStartDate(), r.getEndDate());
        bookingIsMoreThanThreeDays(r.getStartDate(), r.getEndDate());
        bookingIsMoreThan1MonthAway(r.getStartDate());
    }

    private void validateUpdateRequest(UpdateBookingRequest r) {
        if (r.getStartDate() != null || r.getEndDate() != null) {
            // if user wants to update date two dates must be provided
            validateTwoDatesProvided(r.getStartDate(), r.getEndDate());
            validateEndDateLaterStartDate(r.getStartDate(), r.getEndDate());
            bookingIsMoreThanThreeDays(r.getStartDate(), r.getEndDate());
            bookingIsMoreThan1MonthAway(r.getStartDate());
        }
    }

    private UUID fromString(String id) {
        try {
            return UUID.fromString(id);
        } catch (Exception e) {
            throw new ClientValidationException("Can't convert %s to uuid: %s".formatted(id, e.getMessage()));
        }
    }

    private void validateEndDateLaterStartDate(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ClientValidationException("Start date is later than end date");
        }
    }

    public void bookingIsMoreThanThreeDays(LocalDate startDate, LocalDate endDate) {
        if (ChronoUnit.DAYS.between(startDate, endDate) > 3) {
            throw new ClientValidationException("Booking for more than 3 days are not allowed");
        }
    }

    public void bookingIsMoreThan1MonthAway(LocalDate startDate) {
        if (ChronoUnit.MONTHS.between(LocalDate.now(), startDate) > 0) {
            throw new ClientValidationException("No bookings more than 1 month in advance");
        }
    }

    private void validateTwoDatesProvided(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new ClientValidationException("Start date must be provided for booking date update");
        }
        if (endDate == null) {
            throw new ClientValidationException("End date must be provided for booking date update");
        }
    }


}
