package moo.interview.teya.repository;

import moo.interview.teya.entity.MessageQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MessageQueue entities.
 */
@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueue, Long> {

    /**
     * Find all unprocessed messages ordered by creation time ascending.
     *
     * @return List of unprocessed messages
     */
    List<MessageQueue> findByProcessedFalseOrderByCreatedAtInUTCAsc();

    /**
     * Find all unprocessed messages for a specific account.
     *
     * @param accountNumber the account number
     * @return List of unprocessed messages
     */
    List<MessageQueue> findByAccountNumberAndProcessedFalseOrderByCreatedAtInUTCAsc(String accountNumber);
}

