package moo.interview.teya.dto.response;

import moo.interview.teya.entity.TransactionStatus;
import moo.interview.teya.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Transaction.
 */
public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        TransactionStatus status,
        String currency,
        String description,
        Instant createdAtInUTC
) {
}

