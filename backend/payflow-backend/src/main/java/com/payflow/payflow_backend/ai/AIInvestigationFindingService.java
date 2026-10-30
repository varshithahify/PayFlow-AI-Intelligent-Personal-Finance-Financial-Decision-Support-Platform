package com.payflow.payflow_backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.ai.dto.AIInvestigationFindingResponse;
import com.payflow.payflow_backend.ai.dto.AIInvestigationResponse;
import com.payflow.payflow_backend.entity.AIInvestigationFinding;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.repository.AIInvestigationFindingRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AIInvestigationFindingService {

    private static final String MODEL_NAME = "llama3.2:3b";

    private final AIInvestigationFindingRepository findingRepository;
    private final InvestigationRepository investigationRepository;
    private final ObjectMapper objectMapper;

    public AIInvestigationFindingService(
            AIInvestigationFindingRepository findingRepository,
            InvestigationRepository investigationRepository,
            ObjectMapper objectMapper) {

        this.findingRepository = findingRepository;
        this.investigationRepository = investigationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves a validated AI investigation finding for the authenticated
     * organization.
     */
    @Transactional
    public AIInvestigationFinding saveFinding(
            Long transactionId,
            Long investigationId,
            AIInvestigationResponse response) {

        if (transactionId == null || investigationId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID and investigation ID are required");
        }

        validateResponse(response);

        Long orgId = TenantContext.requireOrgId();

        Investigation investigation = investigationRepository
                .findByIdAndOrgId(investigationId, orgId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Investigation not found"));

        if (!transactionId.equals(investigation.getTransactionId())) {
            throw new IllegalArgumentException(
                    "Investigation does not belong to the transaction");
        }

        AIInvestigationFinding finding = new AIInvestigationFinding();

        finding.setOrgId(orgId);
        finding.setInvestigationId(investigationId);
        finding.setTransactionId(transactionId);
        finding.setLikelyCause(response.likelyCause());
        finding.setRequiresHumanReview(true);
        finding.setModelName(MODEL_NAME);

        try {
            finding.setEvidence(
                    objectMapper.writeValueAsString(response.evidence()));

            finding.setRecommendedActions(
                    objectMapper.writeValueAsString(
                            response.recommendedActions()));

            finding.setLimitations(
                    objectMapper.writeValueAsString(
                            response.limitations()));

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize AI investigation findings",
                    exception);
        }

        return findingRepository.save(finding);
    }

    /**
     * Retrieves saved findings only when the requested investigation
     * belongs to the authenticated organization.
     */
    @Transactional(readOnly = true)
    public List<AIInvestigationFindingResponse> getFindings(
            Long investigationId) {

        if (investigationId == null) {
            throw new IllegalArgumentException(
                    "Investigation ID is required");
        }

        Long orgId = TenantContext.requireOrgId();

        // Verify that the investigation belongs to this organization.
        // Return 404 without exposing another organization's data.
        investigationRepository
                .findByIdAndOrgId(investigationId, orgId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Investigation not found"));

        // Retrieve findings scoped to the authenticated organization
        // and the requested investigation.
        List<AIInvestigationFinding> findings =
                findingRepository
                        .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                                orgId,
                                investigationId);

        return findings.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Converts a database entity into the API response DTO.
     */
    private AIInvestigationFindingResponse toResponse(
            AIInvestigationFinding finding) {

        try {
            List<String> evidence = objectMapper.readValue(
                    finding.getEvidence(),
                    new TypeReference<List<String>>() {});

            List<String> recommendedActions = objectMapper.readValue(
                    finding.getRecommendedActions(),
                    new TypeReference<List<String>>() {});

            List<String> limitations = objectMapper.readValue(
                    finding.getLimitations(),
                    new TypeReference<List<String>>() {});

            return new AIInvestigationFindingResponse(
                    finding.getId(),
                    finding.getInvestigationId(),
                    finding.getTransactionId(),
                    finding.getLikelyCause(),
                    evidence,
                    recommendedActions,
                    limitations,
                    finding.isRequiresHumanReview(),
                    finding.getModelName(),
                    finding.getCreatedAt());

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to read saved AI investigation finding",
                    exception);
        }
    }

    /**
     * Validates the AI-generated response before persistence.
     */
    private void validateResponse(AIInvestigationResponse response) {

        if (response == null) {
            throw new IllegalArgumentException(
                    "AI investigation response is required");
        }

        if (!StringUtils.hasText(response.likelyCause())) {
            throw new IllegalArgumentException(
                    "AI finding must include a likely cause");
        }

        validateList(response.evidence(), "evidence");

        validateList(
                response.recommendedActions(),
                "recommended actions");

        validateList(response.limitations(), "limitations");

        if (!response.requiresHumanReview()) {
            throw new IllegalArgumentException(
                    "AI findings must require human review");
        }
    }

    /**
     * Ensures lists are present and contain no blank entries.
     */
    private void validateList(
            List<String> values,
            String fieldName) {

        if (values == null) {
            throw new IllegalArgumentException(
                    "AI finding " + fieldName + " must not be null");
        }

        if (values.stream().anyMatch(
                value -> !StringUtils.hasText(value))) {

            throw new IllegalArgumentException(
                    "AI finding " + fieldName
                            + " must not contain blank entries");
        }
    }
}
