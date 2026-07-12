 package moo.interview.teya.controller;

import moo.interview.teya.dto.response.PageInfo;
import moo.interview.teya.dto.response.TransactionHistoryResponse;
import moo.interview.teya.dto.response.TransactionResponse;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.mapper.TransactionMapper;
import moo.interview.teya.service.LedgerService;
import moo.interview.teya.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private TransactionMapper transactionMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void getTransactionHistory_returnsPagedResponse() throws Exception {
        TransactionHistoryResponse response = new TransactionHistoryResponse(
                List.of(
                        new TransactionResponse(
                                123L,
                                10L,
                                TransactionType.DEPOSIT,
                                new BigDecimal("50.500000"),
                                new BigDecimal("150.500000"),
                                TransactionStatus.COMPLETED,
                                "GBP",
                                "Salary",
                                Instant.parse("2026-07-12T16:20:00Z")
                        )
                ),
                new PageInfo(20, "321", true)
        );

        when(transactionService.getTransactionHistory(
                "ACC-00000001",
                20,
                "123",
                Instant.parse("2026-07-12T00:00:00Z"),
                Instant.parse("2026-07-12T23:59:59Z")
        )).thenReturn(response);

        mockMvc.perform(get("/accounts/ACC-00000001/transactions")
                        .param("pageSize", "20")
                        .param("cursor", "123")
                        .param("fromDate", "2026-07-12T00:00:00Z")
                        .param("toDate", "2026-07-12T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactions[0].id").value(123))
                .andExpect(jsonPath("$.transactions[0].transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.transactions[0].amount").value(50.5))
                .andExpect(jsonPath("$.pageInfo.pageSize").value(20))
                .andExpect(jsonPath("$.pageInfo.nextCursor").value("321"))
                .andExpect(jsonPath("$.pageInfo.hasNextPage").value(true));
    }
}

