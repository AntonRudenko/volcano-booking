package com.upgrade.volcano.integration;

import com.upgrade.volcano.controller.BookingController;
import com.upgrade.volcano.dto.BookingRequest;
import com.upgrade.volcano.service.BookingService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled("Just testing that cache works as intended")
@SpringBootTest
public class AvailabilityCacheTestSuite {

    @Autowired
    private BookingController bookingController;
    @MockBean
    private BookingService bookingService;

    @Test
    public void testGetCache() {
        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(1));
        bookingController.getAvailability(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(1));
        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(1));

        verify(bookingService, times(1)).getBookedDates(LocalDate.now(), LocalDate.now().plusMonths(1));
        verify(bookingService, times(1)).getBookedDates(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(1));
    }

    @Test
    public void testBookCacheEviction() {
        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(2));
        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(2));

        bookingController.book(new BookingRequest(LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), "name", "email@email.com"));

        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(2));
        verify(bookingService, times(2)).getBookedDates(LocalDate.now(), LocalDate.now().plusMonths(2));
    }

    @Test
    public void testCancelCacheEviction() {
        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(3));
        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(3));

        bookingController.cancelBooking(UUID.randomUUID().toString());

        bookingController.getAvailability(LocalDate.now(), LocalDate.now().plusMonths(3));
        verify(bookingService, times(2)).getBookedDates(LocalDate.now(), LocalDate.now().plusMonths(3));
    }


}
