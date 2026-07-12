package moo.interview.teya.service;

import moo.interview.teya.dto.response.BalanceResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class BalanceServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        balanceService = new BalanceService(accountRepository);
    }

    @Test
    void getBalance_returnsCurrentBalanceAndLastUpdatedAt() {
        Instant updatedAt = Instant.parse("2026-07-12T16:00:00Z");
        Account account = Account.builder()
                .id(1L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("123.456789"))
                .currency("GBP")
                .createdAtInUTC(updatedAt.minusSeconds(3600))
                .updatedAtInUTC(updatedAt)
                .build();

        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(account));

        BalanceResponse response = balanceService.getBalance("ACC-00000001");

        assertEquals("ACC-00000001", response.accountNumber());
        assertEquals(new BigDecimal("123.456789"), response.currentBalance());
        assertEquals("GBP", response.currency());
        assertEquals(updatedAt, response.lastUpdatedAtInUTC());
    }

    @Test
    void getBalance_whenAccountMissing_throwsNotFound() {
        when(accountRepository.findByAccountNumber("ACC-404")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> balanceService.getBalance("ACC-404"));
    }
}

