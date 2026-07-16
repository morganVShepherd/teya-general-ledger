package moo.interview.teya.service;

import moo.interview.teya.dto.response.TransactionHistoryResponse;
import moo.interview.teya.dto.response.TransactionResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.mapper.TransactionMapper;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CursorCryptoService cursorCryptoService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionService(accountRepository, transactionRepository, transactionMapper, cursorCryptoService);
    }

    @Test
    void getTransactionHistory_returnsPageWithNextCursor() {
        Account account = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("100.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        Transaction t3 = buildTx(103L);
        Transaction t2 = buildTx(102L);
        Transaction t1 = buildTx(101L);

        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(account));
        when(transactionRepository.findHistoryPage(eq(10L), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(t3, t2, t1));
        when(transactionMapper.toResponse(t3)).thenReturn(buildResponse(103L));
        when(transactionMapper.toResponse(t2)).thenReturn(buildResponse(102L));
        when(cursorCryptoService.encode(102L)).thenReturn("201");

        TransactionHistoryResponse response = transactionService.getTransactionHistory("ACC-00000001", 2, null, null, null);

        assertEquals(2, response.transactions().size());
        assertEquals(2, response.pageInfo().pageSize());
        assertEquals("201", response.pageInfo().nextCursor());
        assertEquals(true, response.pageInfo().hasNextPage());
    }

    @Test
    void getTransactionHistory_withoutNextPage_returnsNullCursor() {
        Account account = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("100.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        Transaction t1 = buildTx(101L);

        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(account));
        when(transactionRepository.findHistoryPage(eq(10L), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(t1));
        when(transactionMapper.toResponse(t1)).thenReturn(buildResponse(101L));

        TransactionHistoryResponse response = transactionService.getTransactionHistory("ACC-00000001", 20, null, null, null);

        assertEquals(1, response.transactions().size());
        assertNull(response.pageInfo().nextCursor());
        assertEquals(false, response.pageInfo().hasNextPage());
    }

    @Test
    void getTransactionHistory_withInvalidPageSize_throwsValidationError() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransactionHistory("ACC-00000001", 0, null, null, null));
    }

    @Test
    void getTransactionHistory_withInvalidDateRange_throwsValidationError() {
        Instant from = Instant.parse("2026-07-12T12:00:00Z");
        Instant to = Instant.parse("2026-07-12T11:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransactionHistory("ACC-00000001", 20, null, from, to));
    }

    @Test
    void getTransactionHistory_withInvalidCursor_throwsValidationError() {
        Account account = Account.builder().id(10L).accountNumber("ACC-00000001").build();
        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(account));
        when(cursorCryptoService.decode("bad-cursor")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransactionHistory("ACC-00000001", 20, "bad-cursor", null, null));
    }

    @Test
    void getTransactionHistory_whenAccountMissing_throwsNotFound() {
        when(accountRepository.findByAccountNumber("ACC-404")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> transactionService.getTransactionHistory("ACC-404", 20, null, null, null));
    }

    @Test
    void getTransactionHistory_withCursor_decodesAndAppliesFilter() {
        Account account = Account.builder().id(10L).accountNumber("ACC-00000001").build();
        Instant from = Instant.parse("2026-07-12T10:00:00Z");
        Instant to = Instant.parse("2026-07-12T12:00:00Z");

        Transaction t1 = buildTx(99L);
        when(accountRepository.findByAccountNumber("ACC-00000001")).thenReturn(Optional.of(account));
        when(cursorCryptoService.decode("321")).thenReturn(123L);
        when(transactionRepository.findHistoryPage(eq(10L), eq(123L), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(List.of(t1));
        when(transactionMapper.toResponse(t1)).thenReturn(buildResponse(99L));

        TransactionHistoryResponse response = transactionService.getTransactionHistory("ACC-00000001", 20, "321", from, to);

        assertNotNull(response);
        assertEquals(1, response.transactions().size());
    }

    private Transaction buildTx(Long id) {
        return Transaction.builder()
                .id(id)
                .accountId(10L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("10.000000"))
                .balanceAfter(new BigDecimal("10.000000"))
                .status(TransactionStatus.COMPLETED)
                .currency("GBP")
                .description("d")
                .createdAtInUTC(Instant.now())
                .build();
    }

    private TransactionResponse buildResponse(Long id) {
        return new TransactionResponse(
                id,
                10L,
                TransactionType.DEPOSIT,
                new BigDecimal("10.000000"),
                new BigDecimal("10.000000"),
                TransactionStatus.COMPLETED,
                "GBP",
                "d",
                Instant.now()
        );
    }
}

