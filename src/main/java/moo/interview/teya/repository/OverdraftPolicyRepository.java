package moo.interview.teya.repository;

import moo.interview.teya.entity.OverdraftPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for OverdraftPolicy entities.
 */
@Repository
public interface OverdraftPolicyRepository extends JpaRepository<OverdraftPolicy, Long> {
    
    /**
     * Find overdraft policy by account ID.
     * 
     * @param accountId the account ID
     * @return Optional containing the overdraft policy if found
     */
    Optional<OverdraftPolicy> findByAccountId(Long accountId);
}

