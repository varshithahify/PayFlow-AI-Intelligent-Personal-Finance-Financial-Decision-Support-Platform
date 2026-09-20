package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.service.InvestigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final InvestigationService investigationService;

    public InvestigationController(
            InvestigationService investigationService) {

        this.investigationService =
                investigationService;
    }

    @PostMapping
    public ResponseEntity<Investigation> createInvestigation(
            @RequestParam Long transactionId,
            @RequestParam(required = false)
            Long reconciliationRecordId,
            @RequestParam
            InvestigationPriority priority,
            @RequestParam
            InvestigationIssueType issueType,
            @RequestParam
            String summary) {

        Investigation investigation =
                investigationService.createInvestigation(
                        transactionId,
                        reconciliationRecordId,
                        priority,
                        issueType,
                        summary);

        return ResponseEntity.ok(investigation);
    }

    @GetMapping("/{investigationId}")
    public ResponseEntity<Investigation> getInvestigation(
            @PathVariable Long investigationId) {

        return ResponseEntity.ok(
                investigationService.getInvestigation(
                        investigationId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Investigation>> getByStatus(
            @PathVariable InvestigationStatus status) {

        return ResponseEntity.ok(
                investigationService.getByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Investigation>> getByPriority(
            @PathVariable InvestigationPriority priority) {

        return ResponseEntity.ok(
                investigationService.getByPriority(priority));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<Investigation>> getByTransactionId(
            @PathVariable Long transactionId) {

        return ResponseEntity.ok(
                investigationService.getByTransactionId(
                        transactionId));
    }

    @PostMapping("/{investigationId}/status")
    public ResponseEntity<Investigation> updateStatus(
            @PathVariable Long investigationId,
            @RequestParam InvestigationStatus status) {

        return ResponseEntity.ok(
                investigationService.updateStatus(
                        investigationId,
                        status));
    }
}