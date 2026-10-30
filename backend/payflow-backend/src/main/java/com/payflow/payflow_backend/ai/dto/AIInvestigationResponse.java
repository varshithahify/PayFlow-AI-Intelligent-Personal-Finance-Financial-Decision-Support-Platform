package com.payflow.payflow_backend.ai.dto;

import java.util.List;

public record AIInvestigationResponse(
        String likelyCause,
        List<String> evidence,
        List<String> recommendedActions,
        boolean requiresHumanReview,
        List<String> limitations
) {
}