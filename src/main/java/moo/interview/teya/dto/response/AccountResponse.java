package moo.interview.teya.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Account.
 */
public record AccountResponse(
        Long id,
        String accountNumber,
        BigDecimal currentBalance,
        String currency,
        Long version,
        Instant createdAtInUTC,
        Instant updatedAtInUTC
) {
}

