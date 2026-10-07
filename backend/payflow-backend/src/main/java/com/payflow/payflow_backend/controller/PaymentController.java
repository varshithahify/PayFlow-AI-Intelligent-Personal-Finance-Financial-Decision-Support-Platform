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
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransactionService transactionService;

    public PaymentController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<TransactionResponse> initiatePayment(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        TransactionResponse createdTransaction =
                transactionService.createTransaction(
                        request,
                        authentication.getName(),
                        idempotencyKey);

        TransactionResponse processedTransaction =
                transactionService.startProcessing(
                        createdTransaction.getId(),
                        authentication.getName());

        return ResponseEntity.ok(processedTransaction);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getPayments(
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.getUserTransactions(
                        authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getPayment(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.getTransaction(
                        id,
                        authentication.getName()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TransactionResponse> refundPayment(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.refundTransaction(
                        id,
                        authentication.getName()));
    }
}