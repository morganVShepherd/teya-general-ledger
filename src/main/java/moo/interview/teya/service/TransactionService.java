package moo.interview.teya.service;

import moo.interview.teya.dto.response.PageInfo;
import moo.interview.teya.dto.response.TransactionHistoryResponse;
import moo.interview.teya.dto.response.TransactionResponse;
import moo.interview.teya.entity.Account;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.mapper.TransactionMapper;
import moo.interview.teya.repository.AccountRepository;
import moo.interview.teya.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CursorCryptoService cursorCryptoService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              CursorCryptoService cursorCryptoService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.cursorCryptoService = cursorCryptoService;
    }

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(String accountNumber,
                                                            int pageSize,
                                                            String cursor,
                                                            Instant fromDate,
                                                            Instant toDate) {
        validateInputs(pageSize, fromDate, toDate);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));

        Long cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            cursorId = cursorCryptoService.decode(cursor);
            if (cursorId == null) {
                throw new IllegalArgumentException("Invalid cursor");
            }
        }

        Pageable pageRequest = PageRequest.of(0, pageSize + 1);
        List<Transaction> page = transactionRepository.findHistoryPage(
                account.getId(),
                cursorId,
                fromDate,
                toDate,
                pageRequest
        );

        boolean hasNextPage = page.size() > pageSize;
        List<Transaction> visible = hasNextPage ? page.subList(0, pageSize) : page;
        String nextCursor = hasNextPage ? cursorCryptoService.encode(visible.get(visible.size() - 1).getId()) : null;

        List<TransactionResponse> responses = visible.stream()
                .map(transactionMapper::toResponse)
                .toList();

        return new TransactionHistoryResponse(
                responses,
                new PageInfo(pageSize, nextCursor, hasNextPage)
        );
    }

    public void processWithdrawal(Transaction transaction){

    }

    private void validateInputs(int pageSize, Instant fromDate, Instant toDate) {
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be less than or equal to toDate");
        }
    }
}

