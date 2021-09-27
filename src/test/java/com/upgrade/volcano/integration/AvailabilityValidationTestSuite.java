package com.upgrade.volcano.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AvailabilityValidationTestSuite {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(BookingScenariosTestSuite.class);

    @Test
    public void startDateNotLaterThanEndDate() throws Exception {
        validateError(LocalDate.now().plusDays(3), LocalDate.now().plusDays(2), "Start date is later than end date");
    }

    @Test
    public void startDateMustBeInTheFuture() throws Exception {
        validateError(LocalDate.now().minusDays(5), LocalDate.now().plusDays(2), "Start date is in the past");
    }

    @Test
    public void oneDateIsNotSet() throws Exception {
        validateError(LocalDate.now().plusDays(5), null, "Start date and end date must be set together");
        validateError(null, LocalDate.now().plusDays(5), "Start date and end date must be set together");
    }


    private void validateError(LocalDate startDate, LocalDate endDate, String message) throws Exception {
        String errorResponseStr = mockMvc.perform(get("/booking")
                        .param("startDate", startDate != null ? startDate.toString() : "")
                        .param("endDate", endDate != null ? endDate.toString() : "")
                ).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        var errorResponse = objectMapper.readValue(errorResponseStr, ErrorResponse.class);
        log.info(errorResponse.toString());
        Assertions.assertEquals(ErrorCode.VALIDATION_ERROR, errorResponse.code());
        Assertions.assertEquals(message, errorResponse.message());

    }

}
