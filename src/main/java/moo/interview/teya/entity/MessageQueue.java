package moo.interview.teya.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MessageQueue entity representing an event in the message queue.
 * Used for asynchronous processing of transactions.
 */
@Entity
@Table(name = "message_queue", indexes = {
        @Index(name = "idx_message_queue_processed_created", columnList = "processed,created_at_in_utc")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "processed", nullable = false)
    private Boolean processed;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "created_at_in_utc", nullable = false)
    private Instant createdAtInUTC;

    @Column(name = "processed_at_in_utc")
    private Instant processedAtInUTC;
}

