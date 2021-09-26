package com.upgrade.volcano.repository;

import com.upgrade.volcano.model.Booking;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingDao extends CrudRepository<Booking, UUID> {
    List<Booking> findAllByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Booking> findAllByBookingId(UUID bookingId);
}
