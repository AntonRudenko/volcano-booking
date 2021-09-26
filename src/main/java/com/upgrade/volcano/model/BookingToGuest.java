package com.upgrade.volcano.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class BookingToGuest {

    // I can use composite primary key for that, but using just id is easier here
    @Id
    @GeneratedValue
    private UUID id;
    private UUID bookingId;
    private UUID guestId;

    public BookingToGuest() {
    }

    public BookingToGuest(UUID bookingId, UUID guestId) {
        this.bookingId = bookingId;
        this.guestId = guestId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID uuid) {
        this.id = uuid;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingid) {
        this.bookingId = bookingid;
    }

    public UUID getGuestId() {
        return guestId;
    }

    public void setGuestId(UUID guestId) {
        this.guestId = guestId;
    }
}
