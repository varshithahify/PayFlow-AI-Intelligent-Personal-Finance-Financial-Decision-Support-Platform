package com.payflow.payflow_backend.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AIInvestigationFindingResponse(
        Long id,
        Long investigationId,
        Long transactionId,
        String likelyCause,
        List<String> evidence,
        List<String> recommendedActions,
        List<String> limitations,
        boolean requiresHumanReview,
        String modelName,
        LocalDateTime createdAt
) {
}

