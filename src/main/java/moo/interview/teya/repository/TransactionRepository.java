package moo.interview.teya.repository;

import moo.interview.teya.entity.Transaction;
import moo.interview.teya.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all transactions for a specific account, ordered by creation time descending.
     * 
     * @param accountId the account ID
     * @param pageable pagination information
     * @return Page of transactions
     */
    Page<Transaction> findByAccountIdOrderByCreatedAtInUTCDesc(Long accountId, Pageable pageable);
    
    /**
     * Find transactions by account ID within a date range.
     * 
     * @param accountId the account ID
     * @param fromDate start date (inclusive)
     * @param toDate end date (inclusive)
     * @param pageable pagination information
     * @return Page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.createdAtInUTC >= :fromDate AND t.createdAtInUTC <= :toDate " +
           "ORDER BY t.createdAtInUTC DESC")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
    
    /**
     * Find pending transactions for an account.
     * 
     * @param accountId the account ID
     * @return List of pending transactions ordered by creation time ascending
     */
    List<Transaction> findByAccountIdAndStatusOrderByCreatedAtInUTCAsc(
            Long accountId,
            TransactionStatus status
    );

    /**
     * Find transaction history for an account using optional cursor and date range filters.
     * Results are ordered by id descending for cursor pagination.
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND (:fromDate IS NULL OR t.createdAtInUTC >= :fromDate) " +
           "AND (:toDate IS NULL OR t.createdAtInUTC <= :toDate) " +
           "AND (:cursorId IS NULL OR t.id < :cursorId) " +
           "ORDER BY t.id DESC")
    List<Transaction> findHistoryPage(
            @Param("accountId") Long accountId,
            @Param("cursorId") Long cursorId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}

