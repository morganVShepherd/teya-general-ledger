package moo.interview.teya.entity.enums;

/**
 * Enum representing the status of a transaction.
 * Transactions are always created as PENDING and only transition to COMPLETED
 * when processed by the message queue job.
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}

