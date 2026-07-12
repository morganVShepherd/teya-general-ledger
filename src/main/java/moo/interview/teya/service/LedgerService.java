package moo.interview.teya.service;

import moo.interview.teya.dto.request.DepositRequest;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.MessageQueue;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.entity.enums.EventType;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.MessageQueueRepository;
import moo.interview.teya.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MessageQueueRepository messageQueueRepository;
    private final MessageQueueService messageQueueService;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999.99").setScale(2);

    public LedgerService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         MessageQueueRepository messageQueueRepository,
                         MessageQueueService messageQueueService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.messageQueueRepository = messageQueueRepository;
        this.messageQueueService = messageQueueService;
    }

    @Transactional
    public Transaction deposit(String accountNumber, DepositRequest request) {
        if (request == null || request.amount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal amount = request.amount().setScale(6);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum allowed");
        }

        // Find account by accountNumber
        Optional<Account> acctOpt = accountRepository.findByAccountNumber(accountNumber);
        if (acctOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }
        Account acct = acctOpt.get();

        // Create transaction (PENDING)
        Transaction tx = Transaction.builder()
                .accountId(acct.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(acct.getCurrentBalance())
                .status(TransactionStatus.PENDING)
                .currency(acct.getCurrency())
                .description(request.description())
                .createdAtInUTC(Instant.now())
                .build();

        Transaction saved = transactionRepository.save(tx);

        // Create message queue entry
        MessageQueue msg = MessageQueue.builder()
                .accountNumber(acct.getAccountNumber())
                .transactionId(saved.getId())
                .eventType(EventType.TRANSACTION_COMPLETED)
                .payload("{}")
                .processed(false)
                .retryCount(0)
                .createdAtInUTC(Instant.now())
                .build();

        MessageQueue savedMsg = messageQueueRepository.save(msg);

        // For deposits, process synchronously to return completed result
        messageQueueService.processNow(savedMsg);

        // reload transaction
        return transactionRepository.findById(saved.getId()).orElse(saved);
    }
}

