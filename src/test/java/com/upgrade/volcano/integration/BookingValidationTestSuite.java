package com.upgrade.volcano.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upgrade.volcano.dto.BookingRequest;
import com.upgrade.volcano.exception.ErrorCode;
import com.upgrade.volcano.dto.ErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookingValidationTestSuite {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(BookingScenariosTestSuite.class);

    @Test
    public void startDateNotLaterThanEndDate() throws Exception {
        var r = new BookingRequest(LocalDate.now().plusDays(3), LocalDate.now().plusDays(2), "name", "email@email.com");
        validateError(r, "Start date is later than end date");
    }

    @Test
    public void startDateMustBeInFuture() throws Exception {
        var r = new BookingRequest(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), "name", "email@email.com");
        validateError(r, "startDate: must be a future date");
    }

    @Test
    public void bookingMustBeLessThan3Days() throws Exception {
        var r = new BookingRequest(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), "name", "email@email.com");
        validateError(r, "Booking for more than 3 days are not allowed");
    }

    @Test
    public void bookingsMustBeWithinMonth() throws Exception {
        var r = new BookingRequest(LocalDate.now().plusMonths(1).plusDays(1), LocalDate.now().plusMonths(1).plusDays(2), "name", "email@email.com");
        validateError(r, "No bookings more than 1 month in advance");
    }

    @Test
    public void bookingMustBeNotToday() throws Exception {
        var r = new BookingRequest(LocalDate.now(), LocalDate.now().plusDays(2), "name", "email@email.com");
        validateError(r, "startDate: must be a future date");
    }

    private void validateError(BookingRequest r, String expectedError) throws Exception {
        String errorResponseStr = mockMvc.perform(post("/booking")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(r))
                ).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        var errorResponse = objectMapper.readValue(errorResponseStr, ErrorResponse.class);
        log.info(errorResponse.toString());
        Assertions.assertEquals(ErrorCode.VALIDATION_ERROR, errorResponse.code());
        Assertions.assertEquals(expectedError, errorResponse.message());
    }

}
