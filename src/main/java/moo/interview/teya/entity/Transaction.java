package moo.interview.teya.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction entity representing a financial transaction.
 * All transactions are created with PENDING status and transition to COMPLETED
 * or FAILED via the message queue processor.
 */
@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_account_created", columnList = "account_id,created_at_in_utc")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at_in_utc", nullable = false)
    private Instant createdAtInUTC;
}

