package com.upgrade.volcano.service;

import com.upgrade.volcano.exception.BookingDoesNotExist;
import com.upgrade.volcano.exception.BookingForDatesExist;
import com.upgrade.volcano.exception.BookingForUserExist;
import com.upgrade.volcano.model.Booking;
import com.upgrade.volcano.model.BookingToGuest;
import com.upgrade.volcano.model.Guest;
import com.upgrade.volcano.repository.BookingDao;
import com.upgrade.volcano.repository.BookingToGuestDao;
import com.upgrade.volcano.repository.GuestDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingDao bookingDao;
    private final GuestDao guestDao;
    private final BookingToGuestDao bookingToGuestDao;

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    public BookingService(BookingDao bookingDao, GuestDao guestDao, BookingToGuestDao bookingToGuestDao) {
        this.bookingDao = bookingDao;
        this.guestDao = guestDao;
        this.bookingToGuestDao = bookingToGuestDao;
    }

    @Transactional
    public UUID book(LocalDate startDate, LocalDate endDate, String email, String name) {
        // business validations
        // booking for email doesn't exist
        Optional<Guest> existingGuest = guestDao.findByEmail(email);
        if (existingGuest.isPresent()) {
            throw new BookingForUserExist(existingGuest.get().getEmail());
        }
        // booking for selected dates does not exist
        List<Booking> existingBookingList = bookingDao.findAllByDateBetween(startDate, endDate);
        if (!existingBookingList.isEmpty()) {
            throw new BookingForDatesExist(existingBookingList.stream().map(Booking::getDate).collect(Collectors.toList()));
        }

        // saving new booking and user
        // generating unique booking id
        // don't worry about uuid uniqueness since I use UUID, risk of getting duplicate is almost impossible
        UUID bookingId = UUID.randomUUID();

        return createFullBooking(startDate, endDate, email, name, bookingId);
    }

    private UUID createFullBooking(LocalDate startDate, LocalDate endDate, String email, String name, UUID bookingId) {
        bookDates(startDate, endDate, bookingId);
        Guest guest = guestDao.save(new Guest(email, name));
        bookingToGuestDao.save(new BookingToGuest(bookingId, guest.getId()));

        return bookingId;
    }

    private void bookDates(LocalDate startDate, LocalDate endDate, UUID bookingId) {
        // for each day of booking we create a booking entity
        List<Booking> bookingList = startDate.datesUntil(endDate.plusDays(1)).map(d -> {
            Booking booking = new Booking();
            booking.setBookingId(bookingId);
            booking.setDate(d);
            return booking;
        }).collect(Collectors.toList());
        bookingDao.saveAll(bookingList);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookedDates(LocalDate startDate, LocalDate endDate) {
        return bookingDao.findAllByDateBetween(startDate, endDate);
    }

    @Transactional
    public void cancel(UUID bookingId) {
        deleteBooking(bookingId);
    }

    private void deleteBooking(UUID bookingId) {
        // checking and deleting bookings
        List<Booking> bookingList = getBooking(bookingId);
        bookingDao.deleteAll(bookingList);

        // let's find user and delete it too alongside with connection
        Optional<UUID> guestId = deleteBookingToGuest(bookingId);
        deleteGuest(guestId);
    }

    // in my notation findXXX can return Optional<XXX>, but getXXX methods throws an exception if it can't find anything
    private List<Booking> getBooking(UUID bookingId) {
        List<Booking> bookingList = bookingDao.findAllByBookingId(bookingId);
        if (bookingList.isEmpty()) {
            throw new BookingDoesNotExist(bookingId);
        }
        return bookingList;
    }


    private void deleteGuest(Optional<UUID> guestId) {
        if (guestId.isEmpty()) {
            // we can't delete something that doesn't exist, we already logged that, so we just exit
            return;
        }

        Optional<Guest> guest = guestDao.findById(guestId.get());
        if (guest.isPresent()) {
            guestDao.delete(guest.get());
        } else {
            // same idea as before, don't fail, log
            log.error("Guest doesn't exist by id %s".formatted(guestId.get()));
        }
    }

    private Optional<UUID> deleteBookingToGuest(UUID bookingId) {
        Optional<BookingToGuest> bookingToGuest = bookingToGuestDao.findByBookingId(bookingId);
        if (bookingToGuest.isPresent()) {
            bookingToGuestDao.delete(bookingToGuest.get());
            return Optional.ofNullable(bookingToGuest.get().getGuestId());
        } else {
            // we have integrity failure, I think we shouldn't throw an error, but this is definetely a problem with
            // creation, so let's log that
            log.error("Booking to guest link doesn't exist by booking id %s".formatted(bookingId));
            return Optional.empty();
        }
    }

    @Transactional
    public void update(UUID bookingId, LocalDate startDate, LocalDate endDate, String email, String name) {
        // we get and check does booking even exist
        List<Booking> booking = getBooking(bookingId);

        // if we are changing the dates, we delete previous booking dates and create new ones
        if (startDate != null && endDate != null) {
            bookingDao.deleteAll(booking);
            bookDates(startDate, endDate, bookingId);
        }

        // if we are changing email or name we are pulling guest entity and updating only what we need
        if (StringUtils.hasText(email) || StringUtils.hasText(name)) {
            updateGuestByBookingId(bookingId, email, name);
        }
    }

    private void updateGuestByBookingId(UUID bookingId, String email, String name) {
        Optional<BookingToGuest> bookingToGuest = bookingToGuestDao.findByBookingId(bookingId);
        // if we can't find booking to guest for some reason and we meant to update it - it is critical error
        if (bookingToGuest.isEmpty()) {
            throw new RuntimeException("Can't find booking to guest by booking id %s".formatted(bookingId));
        }
        Optional<Guest> guest = guestDao.findById(bookingToGuest.get().getGuestId());
        // same logic with a guest
        if (guest.isEmpty()) {
            throw new RuntimeException("Can't find guest by id %s".formatted(bookingToGuest.get().getGuestId()));
        }

        if (StringUtils.hasText(email)) {
            guest.get().setEmail(email);
        }
        if (StringUtils.hasText(name)) {
            guest.get().setName(name);
        }

        guestDao.save(guest.get());
    }
}
