package moo.interview.teya.service;

import moo.interview.teya.dto.response.BalanceResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class BalanceService {

    private final AccountRepository accountRepository;

    public BalanceService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));

        return new BalanceResponse(
                account.getAccountNumber(),
                account.getCurrentBalance(),
                account.getCurrency(),
                account.getUpdatedAtInUTC()
        );
    }
}

