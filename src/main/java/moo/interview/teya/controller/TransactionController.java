package moo.interview.teya.controller;

import moo.interview.teya.dto.request.DepositRequest;
import moo.interview.teya.dto.request.WithdrawalRequest;
import moo.interview.teya.dto.response.TransactionHistoryResponse;
import moo.interview.teya.dto.response.TransactionResponse;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.mapper.TransactionMapper;
import moo.interview.teya.service.LedgerService;
import moo.interview.teya.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/accounts/{accountNumber}/transactions")
public class TransactionController {

    private final LedgerService ledgerService;
    private final TransactionMapper transactionMapper;
    private final TransactionService transactionService;

    public TransactionController(LedgerService ledgerService,
                                 TransactionMapper transactionMapper,
                                 TransactionService transactionService) {
        this.ledgerService = ledgerService;
        this.transactionMapper = transactionMapper;
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable String accountNumber, @RequestBody DepositRequest request) {
        Transaction tx = ledgerService.deposit(accountNumber, request);
        TransactionResponse resp = transactionMapper.toResponse(tx);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@PathVariable String accountNumber, @RequestBody WithdrawalRequest request) {
        Transaction tx = ledgerService.withdraw(accountNumber, request);
        TransactionResponse resp = transactionMapper.toResponse(tx);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    @GetMapping
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber, pageSize, cursor, fromDate, toDate));
    }
}

