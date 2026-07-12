package moo.interview.teya.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Account entity representing a ledger account.
 */
@Entity
@Table(name = "account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "currency", nullable = false)
    private String currency;

    // Version field removed - optimistic locking handled differently or not used

    @Column(name = "created_at_in_utc", nullable = false)
    private Instant createdAtInUTC;

    @Column(name = "updated_at_in_utc", nullable = false)
    private Instant updatedAtInUTC;
}

