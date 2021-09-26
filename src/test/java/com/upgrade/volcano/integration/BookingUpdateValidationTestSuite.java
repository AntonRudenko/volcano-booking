package com.upgrade.volcano.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upgrade.volcano.dto.UpdateBookingRequest;
import com.upgrade.volcano.exception.ErrorCode;
import com.upgrade.volcano.dto.ErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookingUpdateValidationTestSuite {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(BookingScenariosTestSuite.class);


    // testing only additional check
    @Test
    public void twoDatesMustBeProvided() throws Exception {
        var updateRequest = new UpdateBookingRequest();
        updateRequest.setStartDate(LocalDate.now().plusDays(3));
        updateRequest.setEndDate(null);

        String errorResponseStr = mockMvc.perform(put("/booking/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest))
                ).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        var errorResponse = objectMapper.readValue(errorResponseStr, ErrorResponse.class);
        log.info(errorResponse.toString());
        Assertions.assertEquals(ErrorCode.VALIDATION_ERROR, errorResponse.code());
        Assertions.assertEquals("End date must be provided for booking date update", errorResponse.message());

        updateRequest.setStartDate(null);
        updateRequest.setEndDate(LocalDate.now().plusDays(3));

        errorResponseStr = mockMvc.perform(put("/booking/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest))
                ).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        errorResponse = objectMapper.readValue(errorResponseStr, ErrorResponse.class);
        log.info(errorResponse.toString());
        Assertions.assertEquals(ErrorCode.VALIDATION_ERROR, errorResponse.code());
        Assertions.assertEquals("Start date must be provided for booking date update", errorResponse.message());
    }

}
