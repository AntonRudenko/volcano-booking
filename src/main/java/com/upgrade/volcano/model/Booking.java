package com.upgrade.volcano.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;
    private LocalDate date;
    private UUID bookingId;

    public Booking() {
    }

    public Booking(LocalDate date, UUID bookingId) {
        this.date = date;
        this.bookingId = bookingId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

}
