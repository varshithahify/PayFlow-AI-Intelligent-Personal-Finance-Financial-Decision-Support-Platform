package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.createTransaction(request, email)
        );
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.getUserTransactions(email)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.getTransaction(id, email)
        );
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<TransactionResponse> startProcessing(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.startProcessing(id, email)
        );
    }

    @PostMapping("/{id}/success")
    public ResponseEntity<TransactionResponse> markSuccess(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.markSuccess(id, email)
        );
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<TransactionResponse> markFailed(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.markFailed(id, email)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();

        transactionService.deleteTransaction(id, email);

        return ResponseEntity.noContent().build();
    }
}