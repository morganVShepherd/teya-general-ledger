package moo.interview.teya.service;

import moo.interview.teya.dto.response.AccountResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.OverdraftPolicy;
import moo.interview.teya.mapper.AccountMapper;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.OverdraftPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

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
    @Mock
    private OverdraftPolicyRepository overdraftPolicyRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountService = new AccountService(accountRepository, overdraftPolicyRepository, accountMapper, jdbcTemplate);
    }

    @Test
    void createAccount_generatesAccountNumber_andReturnsResponse() {
        when(jdbcTemplate.queryForObject("SELECT NEXT VALUE FOR account_number_seq", Long.class)).thenReturn(1L);

        // Persisted account gets DB id assigned
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

        AccountResponse resp = accountService.createAccount();

        assertNotNull(resp);
        assertEquals("ACC-00000001", resp.accountNumber());

        verify(jdbcTemplate).queryForObject("SELECT NEXT VALUE FOR account_number_seq", Long.class);
        verify(accountRepository).save(any());

        ArgumentCaptor<OverdraftPolicy> overdraftCaptor = ArgumentCaptor.forClass(OverdraftPolicy.class);
        verify(overdraftPolicyRepository).save(overdraftCaptor.capture());
        OverdraftPolicy savedPolicy = overdraftCaptor.getValue();
        assertEquals(1L, savedPolicy.getAccountId());
        assertEquals(false, savedPolicy.getOverdraftAllowed());
        assertEquals(new BigDecimal("0.000000"), savedPolicy.getOverdraftLimit());
    }
}

