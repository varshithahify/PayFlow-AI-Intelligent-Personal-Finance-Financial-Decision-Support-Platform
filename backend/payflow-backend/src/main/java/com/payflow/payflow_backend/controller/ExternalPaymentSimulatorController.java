package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.service.ExternalPaymentSimulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulator")
public class ExternalPaymentSimulatorController {

    private final TransactionRepository transactionRepository;
    private final ExternalPaymentSimulatorService simulatorService;

    public ExternalPaymentSimulatorController(
            TransactionRepository transactionRepository,
            ExternalPaymentSimulatorService simulatorService) {

        this.transactionRepository = transactionRepository;
        this.simulatorService = simulatorService;
    }

    @PostMapping("/external-payment/{transactionId}")
    public ResponseEntity<ExternalPaymentRecord> createExternalPayment(
            @PathVariable Long transactionId,
            @RequestParam String gateway) {

        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found: " + transactionId));

        ExternalPaymentRecord externalRecord =
                simulatorService.createExternalRecord(
                        transaction,
                        gateway);

        return ResponseEntity.ok(externalRecord);
    }
}