package pt.lunasoft.transaction.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.TransactionStatus;
import pt.lunasoft.transaction.service.TransactionService;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction management APIs")
public class TransactionController {

	private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        log.info("Received request to create transaction for account: {}", transaction.getAccountId());
        Transaction created = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<Transaction> getTransaction(@PathVariable UUID id) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account ID")
    public ResponseEntity<Page<Transaction>> getTransactionsByAccount(@PathVariable String accountId, Pageable pageable) {
        Page<Transaction> transactions = transactionService.getTransactionsByAccount(accountId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update transaction status")
    public ResponseEntity<Transaction> updateTransactionStatus(@PathVariable UUID id, @RequestParam TransactionStatus status) {
        Transaction updated = transactionService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(updated);
    }
	
}