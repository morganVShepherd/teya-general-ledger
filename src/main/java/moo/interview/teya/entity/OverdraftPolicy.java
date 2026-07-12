package moo.interview.teya.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OverdraftPolicy entity representing overdraft settings for an account.
 * One-to-one relationship with Account.
 */
@Entity
@Table(name = "overdraft_policy")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdraftPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", unique = true, nullable = false)
    private Long accountId;
    
    @Column(name = "overdraft_allowed", nullable = false)
    private Boolean overdraftAllowed;
    
    @Column(name = "overdraft_limit")
    private BigDecimal overdraftLimit;
    
    @Column(name = "created_at_in_utc", nullable = false)
    private Instant createdAtInUTC;
    
    @Column(name = "updated_at_in_utc", nullable = false)
    private Instant updatedAtInUTC;
}

