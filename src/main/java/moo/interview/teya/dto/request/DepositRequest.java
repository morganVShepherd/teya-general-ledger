package moo.interview.teya.dto.request;

import java.math.BigDecimal;

public record DepositRequest(
        BigDecimal amount,
        String currency,
        String description
) {
}

