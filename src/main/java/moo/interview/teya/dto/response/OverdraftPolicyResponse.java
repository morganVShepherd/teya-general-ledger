package moo.interview.teya.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import moo.interview.teya.util.MoneySerializer;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for OverdraftPolicy.
 */
public record OverdraftPolicyResponse(
        Long id,
        Long accountId,
        Boolean overdraftAllowed,
        @JsonSerialize(using = MoneySerializer.class)
        BigDecimal overdraftLimit,
        Instant createdAtInUTC,
        Instant updatedAtInUTC
) {
}

