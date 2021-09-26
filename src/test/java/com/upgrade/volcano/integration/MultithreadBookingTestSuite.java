package com.upgrade.volcano.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upgrade.volcano.dto.BookingRequest;
import com.upgrade.volcano.exception.ErrorCode;
import com.upgrade.volcano.dto.ErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class MultithreadBookingTestSuite {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void simultaneousBookingTest() throws ExecutionException, InterruptedException {
        CompletableFuture<ErrorCode> future1 = CompletableFuture.supplyAsync(() -> book(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "name", "email@email.com"));
        CompletableFuture<ErrorCode> future2 = CompletableFuture.supplyAsync(() -> book(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "name2", "email2@email.com"));
        CompletableFuture.allOf(future1, future2).join();

        Assertions.assertTrue(ErrorCode.BOOKING_FOR_DATE_EXIST.equals(future1.get()) || ErrorCode.BOOKING_FOR_DATE_EXIST.equals(future2.get()) );
    }

    private ErrorCode book(LocalDate startDate, LocalDate endDate, String name, String email) {
        var bookingRequest = new BookingRequest();
        bookingRequest.setStartDate(startDate);
        bookingRequest.setEndDate(endDate);
        bookingRequest.setName(name);
        bookingRequest.setEmail(email);

        try {
            String errorResponseStr = mockMvc.perform(post("/booking")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(bookingRequest))
                    ).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString();
            var errorResponse = objectMapper.readValue(errorResponseStr, ErrorResponse.class);
            return errorResponse.code();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
