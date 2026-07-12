package moo.interview.teya.controller;

import moo.interview.teya.dto.request.DepositRequest;
import moo.interview.teya.dto.response.TransactionResponse;
import moo.interview.teya.entity.Transaction;
import moo.interview.teya.mapper.TransactionMapper;
import moo.interview.teya.service.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/accounts/{accountNumber}/transactions")
public class TransactionController {

    private final LedgerService ledgerService;
    private final TransactionMapper transactionMapper;

    public TransactionController(LedgerService ledgerService, TransactionMapper transactionMapper) {
        this.ledgerService = ledgerService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable String accountNumber, @RequestBody DepositRequest request) {
        Transaction tx = ledgerService.deposit(accountNumber, request);
        TransactionResponse resp = transactionMapper.toResponse(tx);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}

