package moo.interview.teya.service;

import moo.interview.teya.dto.request.DepositRequest;
import moo.interview.teya.dto.request.WithdrawalRequest;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.MessageQueue;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.entity.enums.EventType;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.exception.CurrencyMismatchException;
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

        WithdrawalRequest req = new WithdrawalRequest(new BigDecimal("200.00"), "GBP", "withdrawal");

        assertThrows(IllegalStateException.class, () -> ledgerService.withdraw("ACC-00000001", req));
    }

    @Test
    void deposit_currencyMismatch_persistsFailedTransactionAndThrows() {
        Account acct = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("100.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(acct));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DepositRequest req = new DepositRequest(new BigDecimal("50.00"), "EUR", "deposit");

        CurrencyMismatchException ex = assertThrows(CurrencyMismatchException.class,
                () -> ledgerService.deposit("ACC-00000001", req));

        assertEquals("Transaction currency must match account currency", ex.getMessage());
        var txCaptor = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals(TransactionStatus.FAILED, txCaptor.getValue().getStatus());
        assertEquals(TransactionType.DEPOSIT, txCaptor.getValue().getTransactionType());
        assertEquals("EUR", txCaptor.getValue().getCurrency());
        verify(messageQueueRepository, never()).save(any());
        verify(messageQueueService, never()).processNow(any());
    }

    @Test
    void withdraw_currencyMismatch_persistsFailedTransactionAndThrows() {
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
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest req = new WithdrawalRequest(new BigDecimal("100.00"), "EUR", "withdrawal");

        CurrencyMismatchException ex = assertThrows(CurrencyMismatchException.class,
                () -> ledgerService.withdraw("ACC-00000001", req));

        assertEquals("Transaction currency must match account currency", ex.getMessage());
        var txCaptor = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals(TransactionStatus.FAILED, txCaptor.getValue().getStatus());
        assertEquals(TransactionType.WITHDRAWAL, txCaptor.getValue().getTransactionType());
        assertEquals("EUR", txCaptor.getValue().getCurrency());
        verify(messageQueueRepository, never()).save(any());
    }

    @Test
    void deposit_missingCurrency_throwsValidationError() {
        DepositRequest req = new DepositRequest(new BigDecimal("50.00"), null, "deposit");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ledgerService.deposit("ACC-00000001", req));

        assertEquals("Currency is required", ex.getMessage());
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

        WithdrawalRequest req = new WithdrawalRequest(new BigDecimal("100.00"), "GBP", "withdrawal");

        Transaction created = ledgerService.withdraw("ACC-00000001", req);
        assertNotNull(created);
        assertEquals(TransactionStatus.PENDING, created.getStatus());
        verify(transactionRepository).save(any());
        verify(messageQueueRepository).save(any(MessageQueue.class));
    }
}

