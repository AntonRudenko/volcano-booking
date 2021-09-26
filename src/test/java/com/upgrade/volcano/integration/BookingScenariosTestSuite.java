package com.upgrade.volcano.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upgrade.volcano.dto.AvailabilityResponse;
import com.upgrade.volcano.dto.BookingRequest;
import com.upgrade.volcano.dto.BookingResponse;
import com.upgrade.volcano.exception.ErrorCode;
import com.upgrade.volcano.dto.ErrorResponse;
import com.upgrade.volcano.model.Booking;
import com.upgrade.volcano.model.BookingToGuest;
import com.upgrade.volcano.model.Guest;
import com.upgrade.volcano.repository.BookingDao;
import com.upgrade.volcano.repository.BookingToGuestDao;
import com.upgrade.volcano.repository.GuestDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookingScenariosTestSuite {

    @Autowired
    private BookingDao bookingDao;
    @Autowired
    private GuestDao guestDao;
    @Autowired
    private BookingToGuestDao bookingToGuestDao;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(BookingScenariosTestSuite.class);

    @BeforeEach
    public void cleanupDb() {
        bookingToGuestDao.deleteAll();
        bookingDao.deleteAll();
        guestDao.deleteAll();
    }

    @Test
    public void fullBookingCycle() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // checking availability
        AvailabilityResponse expectedAvailabilityResponse = new AvailabilityResponse(Collections.emptyList(), startDate, endDate);
        mockMvc.perform(get("/booking")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                ).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedAvailabilityResponse)));

        // booking
        String email = "test@email.com";
        String name = "test";
        UUID bookingId = book(startDate, endDate, name, email);

        // checking that we created entities in db
        List<Booking> createdBookingsById = bookingDao.findAllByBookingId(bookingId);
        // booked for 3 days
        Assertions.assertEquals(3, createdBookingsById.size());

        // check that we booked right dates
        Set<LocalDate> bookedDates = createdBookingsById.stream().map(Booking::getDate).collect(Collectors.toSet());
        startDate.datesUntil(endDate.plusDays(1)).forEach(
                d -> Assertions.assertTrue(bookedDates.contains(d))
        );

        // checking guest created
        Optional<Guest> guest = guestDao.findByEmail(email);
        Assertions.assertTrue(guest.isPresent());
        Assertions.assertEquals(name, guest.get().getName());

        // checking guest is bound to the booking
        Optional<BookingToGuest> bookingToGuest = bookingToGuestDao.findByBookingId(bookingId);
        Assertions.assertTrue(bookingToGuest.isPresent());
        Assertions.assertEquals(guest.get().getId(), bookingToGuest.get().getGuestId());


        // testing update
        LocalDate newStartDate = startDate.plusWeeks(1);
        LocalDate newEndDate = endDate.plusWeeks(1);
        String newName = "testnew";
        String newEmail = "testnew@email.com";
        update(bookingId, newStartDate, newEndDate, newName, newEmail);

        // checking that we created entities in db
        List<Booking> updatedBookingsById = bookingDao.findAllByBookingId(bookingId);
        // booked for 3 days
        Assertions.assertEquals(3, updatedBookingsById.size());

        // check that we booked right dates
        Set<LocalDate> updateBookedDates = updatedBookingsById.stream().map(Booking::getDate).collect(Collectors.toSet());
        newStartDate.datesUntil(newEndDate.plusDays(1)).forEach(
                d -> Assertions.assertTrue(updateBookedDates.contains(d))
        );

        // update guest
        Optional<Guest> updatedGuest = guestDao.findByEmail(newEmail);
        // and check that quest is updated
        Assertions.assertTrue(updatedGuest.isPresent());
        Assertions.assertEquals(newName, updatedGuest.get().getName());


        // testing cancellation
        cancel(bookingId);

        // checking that booking was deleted
        Assertions.assertEquals(0, bookingDao.findAllByBookingId(bookingId).size());
        // checking that guest was deleted
        Assertions.assertFalse(guestDao.findByEmail(newEmail).isPresent());
        // checking that guest to booking was deleted
        Assertions.assertFalse(bookingToGuestDao.findByBookingId(bookingId).isPresent());
    }

    private void update(UUID bookingId, LocalDate newStartDate, LocalDate newEndDate, String name, String email)
            throws Exception {
        var updateRequest = new BookingRequest();

        updateRequest.setStartDate(newStartDate);
        updateRequest.setEndDate(newEndDate);
        updateRequest.setName(name);
        updateRequest.setEmail(email);

        mockMvc.perform(put("/booking/" + bookingId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpect(status().isOk());
    }

    @Test
    public void availabilityAfterBook() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);

        // checking availability
        AvailabilityResponse expectedAvailabilityResponse = new AvailabilityResponse(Collections.emptyList(), startDate, endDate);
        mockMvc.perform(get("/booking")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                ).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedAvailabilityResponse)));

        // booking
        String email = "test@email.com";
        String name = "test";
        UUID bookingId = book(startDate, endDate, name, email);

        //now all should be booked
        startDate.datesUntil(endDate.plusDays(1)).forEach(d -> expectedAvailabilityResponse.getAvailability().put(d, false));

        mockMvc.perform(get("/booking")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                ).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedAvailabilityResponse)));

        cancel(bookingId);
    }

    @Test
    public void bookAfterCancelation() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String email = "test@email.com";
        String name = "test";

        UUID bookingId = book(startDate, endDate, name, email);
        cancel(bookingId);
        book(startDate, endDate, name, email);
    }

    @Test
    public void bookSameDate() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String email = "test_double@email.com";
        String name = "testDouble";
        UUID createdBookingId = book(startDate, endDate, name, email);

        var sameDateBookingRequest = new BookingRequest();
        sameDateBookingRequest.setStartDate(startDate);
        sameDateBookingRequest.setEndDate(endDate);
        sameDateBookingRequest.setName(name);
        // changing email, it is different scenario
        sameDateBookingRequest.setEmail(email + "1");

        String bookingResponseStr = mockMvc.perform(post("/booking")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sameDateBookingRequest))
                ).andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        var errorResponse = objectMapper.readValue(bookingResponseStr, ErrorResponse.class);
        Assertions.assertEquals(ErrorCode.BOOKING_FOR_DATE_EXIST, errorResponse.code());

        var movedBy1DayBookingRequest = new BookingRequest();
        movedBy1DayBookingRequest.setStartDate(startDate.plusDays(1));
        movedBy1DayBookingRequest.setEndDate(endDate.plusDays(1));
        movedBy1DayBookingRequest.setName(name);
        // changing email, it is different scenario
        movedBy1DayBookingRequest.setEmail(email + "2");

        bookingResponseStr = mockMvc.perform(post("/booking")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(movedBy1DayBookingRequest))
                ).andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        errorResponse = objectMapper.readValue(bookingResponseStr, ErrorResponse.class);
        Assertions.assertEquals(ErrorCode.BOOKING_FOR_DATE_EXIST, errorResponse.code());

        cancel(createdBookingId);
    }

    @Test
    public void bookSameEmail() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String email = "test_double@email.com";
        String name = "testDouble";
        UUID createdBookingId = book(startDate, endDate, name, email);

        var sameNameBookingRequest = new BookingRequest();
        sameNameBookingRequest.setStartDate(startDate.plusWeeks(1));
        sameNameBookingRequest.setEndDate(endDate.plusWeeks(1));
        sameNameBookingRequest.setName(name);
        // changing email, it is different scenario
        sameNameBookingRequest.setEmail(email);

        String bookingResponseStr = mockMvc.perform(post("/booking")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sameNameBookingRequest))
                ).andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        var errorResponse = objectMapper.readValue(bookingResponseStr, ErrorResponse.class);
        Assertions.assertEquals(ErrorCode.BOOKING_FOR_USER_EXIST, errorResponse.code());

        cancel(createdBookingId);
    }

    @Test
    public void bookOnSameDateWithDateUpdate() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String email = "test@email.com";
        String name = "test";
        // first we book
        UUID firstBookingId = book(startDate, endDate, name, email);

        // then we update booking to free up dates
        update(firstBookingId, startDate.plusDays(3), endDate.plusDays(3), name, email);

        // and then we book with another user on date that are now available
        UUID secondBookingId = book(startDate, endDate, name, email + "1");

        //lets check that now we have 6 days booked in total
        Assertions.assertEquals(
                6,
                bookingDao.findAllByDateBetween(LocalDate.now(), LocalDate.now().plusMonths(1)).size());

        // cleanup
        cancel(firstBookingId);
        cancel(secondBookingId);
    }

    @Test
    public void bookWithSameEmailAfterEmailUpdate() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String email = "test@email.com";
        String name = "test";
        // first we book
        UUID firstBookingId = book(startDate, endDate, name, email);

        // then we update booking to change email
        update(firstBookingId, null, null, null, email + "new");

        // and then we book with another first user, but on another dates
        UUID secondBookingId = book(startDate.plusDays(3), endDate.plusDays(3), name, email);

        //lets check that now we have 6 days booked in total
        Assertions.assertEquals(
                6,
                bookingDao.findAllByDateBetween(LocalDate.now(), LocalDate.now().plusMonths(1)).size());

        // cleanup
        cancel(firstBookingId);
        cancel(secondBookingId);
    }

    private UUID book(LocalDate startDate, LocalDate endDate, String name, String email) throws Exception {
        var bookingRequest = new BookingRequest();
        bookingRequest.setStartDate(startDate);
        bookingRequest.setEndDate(endDate);
        bookingRequest.setName(name);
        bookingRequest.setEmail(email);

        String bookingResponseStr = mockMvc.perform(post("/booking")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(bookingRequest))
                ).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        BookingResponse bookingResponse = objectMapper.readValue(bookingResponseStr, BookingResponse.class);
        UUID bookingId = bookingResponse.bookingId();
        Assertions.assertNotNull(bookingId);

        return bookingId;
    }

    private void cancel(UUID bookingId) throws Exception {
        mockMvc.perform(delete("/booking/" + bookingId)).andExpect(status().isOk());
    }

    @Test
    public void testBookingUpdate() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String email = "test@email.com";
        String name = "test";
        UUID bookingId = book(startDate, endDate, name, email);

        update(bookingId, null, null, "test2", null);
        Optional<Guest> guest = guestDao.findByEmail(email);
        Assertions.assertEquals("test2", guest.get().getName());

        update(bookingId, null, null, null, "test2@email.com");
        guest = guestDao.findByEmail("test2@email.com");
        Assertions.assertTrue(guest.isPresent());

        update(bookingId, startDate.plusDays(3), endDate.plusDays(3), null, null);
        // check that old dates are empty now
        Assertions.assertEquals(0, bookingDao.findAllByDateBetween(startDate, endDate).size());
        // assert new ones are booked
        Assertions.assertEquals(3, bookingDao.findAllByDateBetween(startDate.plusDays(3), endDate.plusDays(3)).size());
    }
}
