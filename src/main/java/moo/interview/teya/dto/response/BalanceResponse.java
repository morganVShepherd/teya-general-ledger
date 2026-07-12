package moo.interview.teya.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import moo.interview.teya.util.MoneySerializer;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        String accountNumber,
        @JsonSerialize(using = MoneySerializer.class)
        BigDecimal currentBalance,
        String currency,
        Instant lastUpdatedAtInUTC
) {
}

