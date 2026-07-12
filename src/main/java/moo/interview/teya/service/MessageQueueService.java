package moo.interview.teya.service;

import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.MessageQueue;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.MessageQueueRepository;
import moo.interview.teya.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service that processes the message queue. Runs on a scheduled interval and
 * processes unprocessed messages in FIFO order. Synchronized to avoid duplicate processing.
 */
@Service
public class MessageQueueService {

    private static final Logger log = LoggerFactory.getLogger(MessageQueueService.class);
    private static final int MAX_WITHDRAWAL_RETRIES = 3;

    private final MessageQueueRepository messageQueueRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private final Object lock = new Object();

    public MessageQueueService(MessageQueueRepository messageQueueRepository,
                               TransactionRepository transactionRepository,
                               AccountRepository accountRepository) {
        this.messageQueueRepository = messageQueueRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Runs every configured delay (default 1000 ms) and processes unprocessed messages.
     */
    @Scheduled(fixedDelayString = "${teya.message-processor.delay-ms:1000}")
    public void processMessageQueue() {
        // Single lock to avoid concurrent processors
        synchronized (lock) {
            List<MessageQueue> messages = messageQueueRepository.findByProcessedFalseOrderByCreatedAtInUTCAsc();
            if (messages.isEmpty()) {
                return;
            }
            for (MessageQueue msg : messages) {
                try {
                    processMessage(msg);
                } catch (Exception e) {
                    log.error("Failed to process message id={} transactionId={}", msg.getId(), msg.getTransactionId(), e);
                    // increment retry count and continue
                    msg.setRetryCount((msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1);
                    messageQueueRepository.save(msg);
                }
            }
        }
    }

    @Transactional
    protected void processMessage(MessageQueue msg) {
        // If retry needed, implement backoff delay at caller level
        // This method executes single attempt per call from scheduled job
        processMessageInternal(msg);
    }

    private void processMessageInternal(MessageQueue msg) {
        Optional<Transaction> txOpt = transactionRepository.findById(msg.getTransactionId());
        if (txOpt.isEmpty()) {
            log.warn("Transaction not found for message id={} txnId={}", msg.getId(), msg.getTransactionId());
            // mark processed to avoid infinite retry
            msg.setProcessed(true);
            msg.setProcessedAtInUTC(Instant.now());
            messageQueueRepository.save(msg);
            return;
        }

        Transaction tx = txOpt.get();

        if (tx.getStatus() == TransactionStatus.COMPLETED) {
            // Already processed
            msg.setProcessed(true);
            msg.setProcessedAtInUTC(Instant.now());
            messageQueueRepository.save(msg);
            return;
        }

        // Find account
        Optional<Account> acctOpt = accountRepository.findById(tx.getAccountId());
        if (acctOpt.isEmpty()) {
            log.warn("Account not found for transaction id={} accountId={}", tx.getId(), tx.getAccountId());
            // mark message processed to avoid endless loop
            msg.setProcessed(true);
            msg.setProcessedAtInUTC(Instant.now());
            messageQueueRepository.save(msg);
            return;
        }

        Account acct = acctOpt.get();

        // For withdrawals, retry on subsequent scheduler ticks (1s default delay)
        // while prior transactions are still pending.
        if (tx.getTransactionType() == TransactionType.WITHDRAWAL) {
            List<Transaction> allPending = transactionRepository.findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(acct.getId(), TransactionStatus.PENDING);
            // Find all pending transactions BEFORE this one
            List<Transaction> priorPending = allPending.stream()
                    .filter(t -> t.getCreatedAtInUTC().isBefore(tx.getCreatedAtInUTC()))
                    .toList();

            if (!priorPending.isEmpty()) {
                int nextAttempt = (msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1;
                msg.setRetryCount(nextAttempt);

                if (nextAttempt >= MAX_WITHDRAWAL_RETRIES) {
                    log.warn("Withdrawal transaction id={} failed: prior transactions still pending after {} retries", tx.getId(), MAX_WITHDRAWAL_RETRIES);
                    tx.setStatus(TransactionStatus.FAILED);
                    transactionRepository.save(tx);
                    msg.setProcessed(true);
                    msg.setProcessedAtInUTC(Instant.now());
                    messageQueueRepository.save(msg);
                    return;
                } else {
                    log.debug("Withdrawal transaction id={} waiting for prior transactions, retry={}/{}", tx.getId(), nextAttempt, MAX_WITHDRAWAL_RETRIES);
                    messageQueueRepository.save(msg);
                    return;
                }
            }
        }

        // Apply transaction to account balance
        BigDecimal current = acct.getCurrentBalance();
        BigDecimal amount = tx.getAmount();
        BigDecimal newBalance = current;

        if (tx.getTransactionType() == TransactionType.DEPOSIT) {
            newBalance = current.add(amount);
        } else if (tx.getTransactionType() == TransactionType.WITHDRAWAL) {
            newBalance = current.subtract(amount);
        }

        // Update transaction
        tx.setBalanceAfter(newBalance);
        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        // Update account
        acct.setCurrentBalance(newBalance);
        acct.setUpdatedAtInUTC(Instant.now());
        accountRepository.save(acct);

        // Mark message processed
        msg.setProcessed(true);
        msg.setProcessedAtInUTC(Instant.now());
        messageQueueRepository.save(msg);

        log.info("Processed message id={} txnId={} account={} newBalance={}", msg.getId(), tx.getId(), acct.getAccountNumber(), newBalance);
    }

    /**
     * Public helper to process a single message immediately (used by synchronous flows e.g. deposits).
     */
    public void processNow(MessageQueue msg) {
        processMessage(msg);
    }
}

