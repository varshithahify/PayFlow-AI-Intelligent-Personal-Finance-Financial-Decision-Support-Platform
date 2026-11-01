package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.service.TransactionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.createTransaction(
                        request,
                        authentication.getName(),
                        idempotencyKey));
    }

    // Existing endpoint - backward compatibility
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.getUserTransactions(
                        authentication.getName()));
    }

    // Paginated transaction listing with server-side filtering
    @GetMapping("/page")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsPage(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(required = false) String search,

            @RequestParam(required = false) String status,

            @RequestParam(required = false) String paymentMethod,

            @RequestParam(defaultValue = "createdAt") String sortBy,

            @RequestParam(defaultValue = "desc") String direction,

            Authentication authentication) {

        // Validate page number
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        // Validate page size
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        // Validate sorting fields
        List<String> allowedSortFields = List.of(
                "id",
                "amount",
                "status",
                "createdAt"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy);
        }

        // Validate sorting direction
        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Direction must be either 'asc' or 'desc'");
        }

        // Convert status string into TransactionStatus enum
        TransactionStatus transactionStatus = null;

        if (status != null && !status.isBlank()) {
            try {
                transactionStatus = TransactionStatus.valueOf(
                        status.trim().toUpperCase());

            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid transaction status: " + status);
            }
        }

        // Normalize payment method
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            paymentMethod = paymentMethod.trim().toUpperCase();
        } else {
            paymentMethod = null;
        }

        // Create pagination and sorting configuration
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        // Fetch filtered and paginated transactions
        return ResponseEntity.ok(
                transactionService.getUserTransactions(
                        authentication.getName(),
                        search,
                        transactionStatus,
                        paymentMethod,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.getTransaction(
                        id,
                        authentication.getName()));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<TransactionResponse> startProcessing(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.startProcessing(
                        id,
                        authentication.getName()));
    }

    @PostMapping("/{id}/success")
    public ResponseEntity<TransactionResponse> markSuccess(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.markSuccess(
                        id,
                        authentication.getName()));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<TransactionResponse> markFailed(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.markFailed(
                        id,
                        authentication.getName()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TransactionResponse> refundTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                transactionService.refundTransaction(
                        id,
                        authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        transactionService.deleteTransaction(
                id,
                authentication.getName());

        return ResponseEntity.noContent().build();
    }
}