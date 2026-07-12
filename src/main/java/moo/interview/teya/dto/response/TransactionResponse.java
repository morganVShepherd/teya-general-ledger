package moo.interview.teya.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import moo.interview.teya.entity.enums.TransactionStatus;
import moo.interview.teya.entity.enums.TransactionType;
import moo.interview.teya.util.MoneySerializer;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Transaction.
 */
public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType transactionType,
        @JsonSerialize(using = MoneySerializer.class)
        BigDecimal amount,
        @JsonSerialize(using = MoneySerializer.class)
        BigDecimal balanceAfter,
        TransactionStatus status,
        String currency,
        String description,
        Instant createdAtInUTC
) {
}

