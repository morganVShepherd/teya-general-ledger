package moo.interview.teya.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for OverdraftPolicy.
 */
public record OverdraftPolicyResponse(
        Long id,
        Long accountId,
        Boolean overdraftAllowed,
        BigDecimal overdraftLimit,
        Instant createdAtInUTC,
        Instant updatedAtInUTC
) {
}

