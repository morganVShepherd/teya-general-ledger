package moo.interview.teya.dto.request;

import java.math.BigDecimal;

public record WithdrawalRequest(
        BigDecimal amount,
        String currency,
        String description
) {
}

