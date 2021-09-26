package com.upgrade.volcano.integration;

import com.upgrade.volcano.model.Booking;
import com.upgrade.volcano.repository.BookingDao;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc // not needed for mvc test, but n without it spring will try to initialize DB once more
public class BookingDaoTestSuite {

    @Autowired
    private BookingDao bookingDao;

    @Test
    public void bookingOnTheSameDay() {
        Booking booking = new Booking();
        booking.setDate(LocalDate.now().plusDays(3));
        booking.setBookingId(UUID.randomUUID());

        Booking booking2 = new Booking();
        booking2.setDate(LocalDate.now().plusDays(3));
        booking2.setBookingId(UUID.randomUUID());

        bookingDao.save(booking);
        Exception exception = null;
        try {
            bookingDao.save(booking2);
        } catch (DataIntegrityViolationException e) {
            // exception is expected due to db constraints
            // checking that we triggered right constraint
            String constraintName = ((ConstraintViolationException) e.getCause()).getConstraintName();
            Assertions.assertTrue(constraintName.toLowerCase().contains("unique_booking_date"));
            exception = e;
        }
        Assertions.assertNotNull(exception);

        bookingDao.delete(booking);
    }

}
