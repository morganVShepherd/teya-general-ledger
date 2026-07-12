package moo.interview.teya.repository;

import moo.interview.teya.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Account entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find an account by account number.
     *
     * @param accountNumber the account number
     * @return Optional containing the account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);
}

