package moo.interview.teya.service;

import moo.interview.teya.dto.response.AccountResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.OverdraftPolicy;
import moo.interview.teya.mapper.AccountMapper;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.OverdraftPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final OverdraftPolicyRepository overdraftPolicyRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository,
                          OverdraftPolicyRepository overdraftPolicyRepository,
                          AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.overdraftPolicyRepository = overdraftPolicyRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountResponse createAccount() {
            Instant now = Instant.now();
            Long generatedValue = Objects.requireNonNull(
                    accountRepository.getNextAccountNumberValue(),
                    "Failed to generate account number value"
            );
            String accountNumber = String.format("ACC-%04d", generatedValue);

            Account acct = Account.builder()
                    .accountNumber(accountNumber)
                    .currentBalance(BigDecimal.ZERO.setScale(6))
                    .currency("GBP")
                    .createdAtInUTC(now)
                    .updatedAtInUTC(now)
                    .build();

            Account saved = accountRepository.save(acct);

            OverdraftPolicy defaultPolicy = OverdraftPolicy.builder()
                    .accountId(saved.getId())
                    .overdraftAllowed(false)
                    .overdraftLimit(BigDecimal.ZERO.setScale(6))
                    .createdAtInUTC(now)
                    .updatedAtInUTC(now)
                    .build();
            overdraftPolicyRepository.save(defaultPolicy);

            return accountMapper.toResponse(saved);
    }
}

