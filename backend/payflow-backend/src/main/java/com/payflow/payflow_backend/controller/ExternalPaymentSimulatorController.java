package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;
import com.payflow.payflow_backend.service.ExternalPaymentSimulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;

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

@PreAuthorize("hasRole('MERCHANT_ADMIN')")
@PostMapping("/external-payment/{transactionId}")
public ResponseEntity<ExternalPaymentRecord> createExternalPayment(
        @PathVariable Long transactionId,
        @RequestParam String gateway) {

    Long orgId = TenantContext.requireOrgId();

    Transaction transaction = transactionRepository
            .findByIdAndOrgId(transactionId, orgId)
            .orElseThrow(() ->
        new ResourceNotFoundException(
                "Transaction not found: " + transactionId));

    ExternalPaymentRecord externalRecord =
            simulatorService.createExternalRecord(
                    transaction,
                    gateway);

    return ResponseEntity.ok(externalRecord);
}


}
