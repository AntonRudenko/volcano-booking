package com.upgrade.volcano.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import java.time.LocalDate;

public class UpdateBookingRequest {

    @Future
    private LocalDate startDate;
    @Future
    private LocalDate endDate;
    private String name;
    @Email
    private String email;

    public UpdateBookingRequest() {
    }

    public UpdateBookingRequest(LocalDate startDate, LocalDate endDate, String name, String email) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        this.email = email;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
