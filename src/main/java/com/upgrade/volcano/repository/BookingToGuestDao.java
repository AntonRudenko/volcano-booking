package com.upgrade.volcano.repository;

import com.upgrade.volcano.model.BookingToGuest;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingToGuestDao extends CrudRepository<BookingToGuest, UUID> {
    Optional<BookingToGuest> findByBookingId(UUID bookingId);
}
