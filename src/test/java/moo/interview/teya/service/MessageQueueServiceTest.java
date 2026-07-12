package moo.interview.teya.service;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessageQueueServiceTest {

    @Mock
    private MessageQueueRepository messageQueueRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;

    private MessageQueueService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MessageQueueService(messageQueueRepository, transactionRepository, accountRepository);
    }

    @Test
    void processMessage_transactionNotFound_marksMessageProcessed() {
        MessageQueue msg = MessageQueue.builder()
                .id(1L)
                .transactionId(100L)
                .accountNumber("ACC-00000001")
                .eventType(EventType.TRANSACTION_COMPLETED)
                .payload("{}")
                .processed(false)
                .retryCount(0)
                .createdAtInUTC(Instant.now())
                .build();

        when(transactionRepository.findById(100L)).thenReturn(Optional.empty());

        service.processMessage(msg);

        assertTrue(msg.getProcessed());
        assertNotNull(msg.getProcessedAtInUTC());
        verify(messageQueueRepository).save(msg);
    }

    @Test
    void processMessage_pendingTransaction_appliesToAccountAndMarksProcessed() {
        // Setup account
        Account acct = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("100.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        // Setup transaction
        Transaction tx = Transaction.builder()
                .id(200L)
                .accountId(10L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("50.000000"))
                .balanceAfter(BigDecimal.ZERO)
                .status(TransactionStatus.PENDING)
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .build();

        MessageQueue msg = MessageQueue.builder()
                .id(2L)
                .transactionId(200L)
                .accountNumber("ACC-00000001")
                .eventType(EventType.TRANSACTION_COMPLETED)
                .payload("{}")
                .processed(false)
                .retryCount(0)
                .createdAtInUTC(Instant.now())
                .build();

        when(transactionRepository.findById(200L)).thenReturn(Optional.of(tx));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(acct));
        when(transactionRepository.findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(10L, TransactionStatus.PENDING)).thenReturn(List.of(tx));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageQueueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.processMessage(msg);

        // verify transaction marked completed and balance updated
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(TransactionStatus.COMPLETED, savedTx.getStatus());
        assertEquals(new BigDecimal("150.000000"), savedTx.getBalanceAfter());

        // verify account updated
        ArgumentCaptor<Account> acctCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(acctCaptor.capture());
        Account savedAcct = acctCaptor.getValue();
        assertEquals(new BigDecimal("150.000000"), savedAcct.getCurrentBalance());

        // message marked processed
        assertTrue(msg.getProcessed());
        assertNotNull(msg.getProcessedAtInUTC());
        verify(messageQueueRepository).save(msg);
    }

    @Test
    void processMessage_withdrawal_withPriorPending_increments_retryCount() {
        Account acct = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("500.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        Instant now = Instant.now();
        // Prior deposit still pending
        Transaction priorDeposit = Transaction.builder()
                .id(100L)
                .accountId(10L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100.000000"))
                .status(TransactionStatus.PENDING)
                .currency("GBP")
                .createdAtInUTC(now.minusSeconds(10))
                .build();

        // Withdrawal created after prior deposit
        Transaction withdrawal = Transaction.builder()
                .id(200L)
                .accountId(10L)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("50.000000"))
                .status(TransactionStatus.PENDING)
                .currency("GBP")
                .createdAtInUTC(now)
                .build();

        MessageQueue msg = MessageQueue.builder()
                .id(3L)
                .transactionId(200L)
                .accountNumber("ACC-00000001")
                .eventType(EventType.TRANSACTION_COMPLETED)
                .payload("{}")
                .processed(false)
                .retryCount(0)
                .createdAtInUTC(now)
                .build();

        when(transactionRepository.findById(200L)).thenReturn(Optional.of(withdrawal));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(acct));
        when(transactionRepository.findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(10L, TransactionStatus.PENDING))
                .thenReturn(List.of(priorDeposit, withdrawal));
        when(messageQueueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.processMessage(msg);

        // Verify retry count incremented and message NOT marked processed
        assertEquals(1, msg.getRetryCount());
        assertFalse(msg.getProcessed());
        verify(messageQueueRepository).save(msg);
    }

    @Test
    void processMessage_withdrawal_onThirdRetry_marksFailed() {
        Account acct = Account.builder()
                .id(10L)
                .accountNumber("ACC-00000001")
                .currentBalance(new BigDecimal("500.000000"))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build();

        Instant now = Instant.now();
        Transaction priorDeposit = Transaction.builder()
                .id(100L)
                .accountId(10L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100.000000"))
                .status(TransactionStatus.PENDING)
                .currency("GBP")
                .createdAtInUTC(now.minusSeconds(10))
                .build();

        Transaction withdrawal = Transaction.builder()
                .id(200L)
                .accountId(10L)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("50.000000"))
                .status(TransactionStatus.PENDING)
                .currency("GBP")
                .createdAtInUTC(now)
                .build();

        MessageQueue msg = MessageQueue.builder()
                .id(4L)
                .transactionId(200L)
                .accountNumber("ACC-00000001")
                .eventType(EventType.TRANSACTION_COMPLETED)
                .payload("{}")
                .processed(false)
                .retryCount(2)
                .createdAtInUTC(now)
                .build();

        when(transactionRepository.findById(200L)).thenReturn(Optional.of(withdrawal));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(acct));
        when(transactionRepository.findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(10L, TransactionStatus.PENDING))
                .thenReturn(List.of(priorDeposit, withdrawal));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageQueueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.processMessage(msg);

        // Verify withdrawal marked as FAILED
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals(TransactionStatus.FAILED, txCaptor.getValue().getStatus());

        // Message marked as processed
        assertTrue(msg.getProcessed());
        assertNotNull(msg.getProcessedAtInUTC());
    }
}

