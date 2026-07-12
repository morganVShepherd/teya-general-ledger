package moo.interview.teya.dto.response;

import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;

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

