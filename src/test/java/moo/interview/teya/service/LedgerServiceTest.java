package moo.interview.teya.service;

import moo.interview.teya.dto.request.WithdrawalRequest;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.MessageQueue;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.entity.enums.EventType;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.MessageQueueRepository;
import moo.interview.teya.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MessageQueueRepository messageQueueRepository;
    @Mock
    private MessageQueueService messageQueueService;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ledgerService = new LedgerService(accountRepository, transactionRepository, messageQueueRepository, messageQueueService);
    }

    @Test
    void withdraw_insufficientBalance_throws() {
        Account acct = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("100.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(acct));
        when(transactionRepository.findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(10L, TransactionStatus.PENDING)).thenReturn(List.of());

        WithdrawalRequest req = new WithdrawalRequest(new BigDecimal("200.00"), "withdrawal");

        assertThrows(IllegalStateException.class, () -> ledgerService.withdraw("ACC-00000001", req));
    }

    @Test
    void withdraw_sufficient_createsPendingTransactionAndMessage() {
        Account acct = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("500.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(acct));
        when(transactionRepository.findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(10L, TransactionStatus.PENDING)).thenReturn(List.of());
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(300L);
            return t;
        });
        when(messageQueueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest req = new WithdrawalRequest(new BigDecimal("100.00"), "withdrawal");

        Transaction created = ledgerService.withdraw("ACC-00000001", req);
        assertNotNull(created);
        assertEquals(TransactionStatus.PENDING, created.getStatus());
        verify(transactionRepository).save(any());
        verify(messageQueueRepository).save(any(MessageQueue.class));
    }
}

