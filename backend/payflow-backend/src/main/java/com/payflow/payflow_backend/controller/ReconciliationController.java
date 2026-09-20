package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService) {

        this.reconciliationService =
                reconciliationService;
    }

    @PostMapping("/transactions/{transactionId}")
    public ResponseEntity<ReconciliationRecord> reconcileTransaction(
            @PathVariable Long transactionId) {

        ReconciliationRecord record =
                reconciliationService.reconcileTransaction(
                        transactionId);

        return ResponseEntity.ok(record);
    }

    @PostMapping("/external-records/{externalRecordId}")
    public ResponseEntity<ReconciliationRecord> reconcileExternalRecord(
            @PathVariable Long externalRecordId) {

        ReconciliationRecord record =
                reconciliationService.reconcileExternalRecord(
                        externalRecordId);

        return ResponseEntity.ok(record);
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ReconciliationRecord> getLatestReconciliation(
            @PathVariable Long transactionId) {

        return reconciliationService
                .getLatestReconciliation(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReconciliationRecord>> getByStatus(
            @PathVariable ReconciliationStatus status) {

        return ResponseEntity.ok(
                reconciliationService.getByStatus(status));
    }
}