package moo.interview.teya.service;

import moo.interview.teya.dto.request.CreateAccountRequest;
import moo.interview.teya.dto.response.AccountResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.mapper.AccountMapper;
import moo.interview.teya.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountMapper accountMapper;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountService(accountRepository, accountMapper);
    }

    @Test
    void createAccount_generatesAccountNumber_andReturnsResponse() {
        // Prepare saved account after first save (id assigned)
        when(accountRepository.save(any())).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            if (a.getId() == null) {
                a.setId(1L);
            }
            return a;
        });

        Account saved = new Account();
        saved.setId(1L);
        saved.setAccountNumber("ACC-00000001");
        saved.setCurrentBalance(new BigDecimal("0.000000"));
        saved.setCurrency("GBP");
        saved.setCreatedAtInUTC(Instant.now());
        saved.setUpdatedAtInUTC(Instant.now());

        when(accountMapper.toResponse(any())).thenReturn(new AccountResponse(
                saved.getId(), saved.getAccountNumber(), saved.getCurrentBalance(), saved.getCurrency(), saved.getCreatedAtInUTC(), saved.getUpdatedAtInUTC()
        ));

        AccountResponse resp = accountService.createAccount(new CreateAccountRequest());

        assertNotNull(resp);
        assertEquals("ACC-00000001", resp.accountNumber());

        // Verify accountRepository.save called at least once
        verify(accountRepository, atLeast(1)).save(any());
    }
}

