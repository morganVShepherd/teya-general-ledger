package moo.interview.teya.repository;

import moo.interview.teya.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests proving that the {@code @Version} field on {@link Account} provides optimistic locking.
 *
 * <p>How it works:
 * <ol>
 *   <li>Every {@code Account} row has a {@code version} column (starts at 0).</li>
 *   <li>On every save JPA runs:
 *       {@code UPDATE account SET ..., version = version+1 WHERE id = ? AND version = <old>}</li>
 *   <li>If two threads both read version=0, the first writer wins (version becomes 1).
 *       The second writer's UPDATE checks WHERE version=0, finds 0 rows, and throws
 *       {@link ObjectOptimisticLockingFailureException} — preventing a silent lost update.</li>
 * </ol>
 *
 * <p>{@code @DataJpaTest} loads the full Liquibase schema (including the {@code version} column)
 * against an in-memory H2 database. Each test runs in its own transaction that is rolled back
 * afterwards, so tests are fully isolated.
 */
@DataJpaTest
class AccountOptimisticLockingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    /** Direct SQL lets us simulate a concurrent writer bypassing the JPA first-level cache. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account account;

    @BeforeEach
    void setUp() {
        account = accountRepository.save(Account.builder()
                .accountNumber("OPT-0001")
                .currentBalance(BigDecimal.ZERO.setScale(6))
                .currency("GBP")
                .createdAtInUTC(Instant.now())
                .updatedAtInUTC(Instant.now())
                .build());
        entityManager.flush();
    }

    // ── 1. Version starts at zero ────────────────────────────────────────────

    @Test
    void version_isZero_afterFirstSave() {
        assertEquals(0L, account.getVersion(),
                "A newly created account should start with version 0");
    }

    // ── 2. Version auto-increments on every update ───────────────────────────

    @Test
    void version_incrementsToOne_afterFirstUpdate() {
        account.setCurrentBalance(new BigDecimal("100.000000"));
        accountRepository.save(account);
        entityManager.flush();
        entityManager.clear(); // evict from 1st-level cache so the reload hits the DB

        Account reloaded = accountRepository.findByAccountNumber("OPT-0001").orElseThrow();
        assertEquals(1L, reloaded.getVersion(),
                "Version should be 1 after the first update");
    }

    @Test
    void version_incrementsOnEverySubsequentUpdate() {
        // 0 -> 1
        account.setCurrentBalance(new BigDecimal("100.000000"));
        accountRepository.save(account);
        entityManager.flush();
        entityManager.clear();

        // 1 -> 2
        Account v1 = accountRepository.findByAccountNumber("OPT-0001").orElseThrow();
        v1.setCurrentBalance(new BigDecimal("200.000000"));
        accountRepository.save(v1);
        entityManager.flush();
        entityManager.clear();

        Account v2 = accountRepository.findByAccountNumber("OPT-0001").orElseThrow();
        assertEquals(2L, v2.getVersion(),
                "Version should be 2 after two successive updates");
    }

    // ── 3. Stale entity is rejected ──────────────────────────────────────────

    /**
     * Proves the lost-update protection:
     *
     * <pre>
     *   Thread A  -- read (version=0) ----------------------- save --> EXCEPTION
     *   Thread B  -- read (version=0) -- save (version=1) --> OK
     * </pre>
     *
     * Thread B is simulated by a direct JDBC UPDATE that bumps the version in the
     * database without touching the JPA first-level cache, so Thread A's reference
     * still holds version=0 (stale).
     *
     * When JPA flushes Thread A's change it executes:
     * <pre>
     *   UPDATE account SET current_balance=?, version=1 WHERE id=? AND version=0
     * </pre>
     * WHERE version=0 finds no row (DB already has version=1), Hibernate detects
     * 0 rows updated and throws {@link ObjectOptimisticLockingFailureException}.
     */
    @Test
    void savingStaleEntity_throwsOptimisticLockException() {
        // Thread B commits first — bump version directly in DB, bypassing the JPA cache
        jdbcTemplate.update(
                "UPDATE account SET version = version + 1, current_balance = 100.000000 WHERE id = ?",
                account.getId());

        // Thread A still holds version=0; it mutates and tries to flush
        account.setCurrentBalance(new BigDecimal("999.000000"));

        // saveAndFlush goes through Spring Data's exception translation layer, which
        // wraps Hibernate's OptimisticLockException in ObjectOptimisticLockingFailureException.
        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                accountRepository.saveAndFlush(account),
                "Saving a stale entity must throw ObjectOptimisticLockingFailureException");
    }

    // ── 4. Sequential updates without conflict always succeed ────────────────

    @Test
    void sequentialUpdates_withNoConflict_succeedWithoutException() {
        assertDoesNotThrow(() -> {
            Account first = accountRepository.findByAccountNumber("OPT-0001").orElseThrow();
            first.setCurrentBalance(new BigDecimal("50.000000"));
            accountRepository.save(first);
            entityManager.flush();
            entityManager.clear();

            Account second = accountRepository.findByAccountNumber("OPT-0001").orElseThrow();
            second.setCurrentBalance(new BigDecimal("75.000000"));
            accountRepository.save(second);
            entityManager.flush();
        });
    }
}

