package com.upgrade.volcano.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class BookingRequest {

    @Future
    @NotNull
    private LocalDate startDate;
    @Future
    @NotNull
    private LocalDate endDate;
    @NotBlank
    private String name;
    @Email
    @NotBlank
    private String email;

    public BookingRequest() {
    }

    public BookingRequest(LocalDate startDate, LocalDate endDate, String name, String email) {
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
