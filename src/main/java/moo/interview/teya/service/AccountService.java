package moo.interview.teya.service;

import moo.interview.teya.dto.request.CreateAccountRequest;
import moo.interview.teya.dto.response.AccountResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Instant now = Instant.now();
        Account acct = Account.builder()
                .accountNumber(null)
                .currentBalance(BigDecimal.ZERO.setScale(6))
                .currency("GBP")
                .createdAtInUTC(now)
                .updatedAtInUTC(now)
                .build();

        // Persist to get ID
        Account saved = accountRepository.save(acct);

        // Generate account number using ID
        String accountNumber = String.format("ACC-%08d", saved.getId());
        saved.setAccountNumber(accountNumber);
        saved = accountRepository.save(saved);

        return accountMapper.toResponse(saved);
    }
}

