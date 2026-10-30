package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.ai.AIInvestigationFindingService;
import com.payflow.payflow_backend.ai.AIInvestigationService;
import com.payflow.payflow_backend.ai.dto.AIInvestigationFindingResponse;
import com.payflow.payflow_backend.ai.dto.AIInvestigationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/investigations")
public class AIInvestigationController {

    private final AIInvestigationService aiInvestigationService;
    private final AIInvestigationFindingService findingService;

    public AIInvestigationController(
            AIInvestigationService aiInvestigationService,
            AIInvestigationFindingService findingService) {

        this.aiInvestigationService = aiInvestigationService;
        this.findingService = findingService;
    }

    @PostMapping("/{transactionId}")
    public ResponseEntity<AIInvestigationResponse> investigateTransaction(
            @PathVariable Long transactionId) {

        AIInvestigationResponse response =
                aiInvestigationService.investigate(transactionId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{investigationId}/findings")
    public ResponseEntity<List<AIInvestigationFindingResponse>> getFindings(
            @PathVariable Long investigationId) {

        List<AIInvestigationFindingResponse> findings =
                findingService.getFindings(investigationId);

        return ResponseEntity.ok(findings);
    }
}

