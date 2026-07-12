package moo.interview.teya.controller;

import moo.interview.teya.dto.response.BalanceResponse;
import moo.interview.teya.service.BalanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BalanceService balanceService;

    @Test
    void getBalance_returnsOkResponse() throws Exception {
        when(balanceService.getBalance("ACC-00000001")).thenReturn(
                new BalanceResponse(
                        "ACC-00000001",
                        new BigDecimal("150.987654"),
                        "GBP",
                        Instant.parse("2026-07-12T16:15:30Z")
                )
        );

        mockMvc.perform(get("/accounts/ACC-00000001/balance"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value("ACC-00000001"))
                .andExpect(jsonPath("$.currentBalance").value("150.98"))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.lastUpdatedAtInUTC").value("2026-07-12T16:15:30Z"));
    }
}

