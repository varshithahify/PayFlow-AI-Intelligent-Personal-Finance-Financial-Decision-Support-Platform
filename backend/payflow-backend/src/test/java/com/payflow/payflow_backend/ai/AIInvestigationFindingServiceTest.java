package com.payflow.payflow_backend.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.ai.dto.AIInvestigationFindingResponse;
import com.payflow.payflow_backend.entity.AIInvestigationFinding;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.repository.AIInvestigationFindingRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIInvestigationFindingServiceTest {

    @Mock
    private AIInvestigationFindingRepository findingRepository;

    @Mock
    private InvestigationRepository investigationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AIInvestigationFindingService findingService;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnFindingsForAuthorizedOrganization() throws Exception {
        Long orgId = 1L;
        Long investigationId = 14L;

        Investigation investigation = new Investigation();
        investigation.setId(investigationId);
        investigation.setOrgId(orgId);
        investigation.setTransactionId(20L);

        AIInvestigationFinding finding =
                mock(AIInvestigationFinding.class);

        when(finding.getId()).thenReturn(1L);
        when(finding.getInvestigationId()).thenReturn(investigationId);
        when(finding.getTransactionId()).thenReturn(20L);
        when(finding.getLikelyCause())
                .thenReturn("No matching external payment record found");
        when(finding.getEvidence())
                .thenReturn("[\"External record missing\"]");
        when(finding.getRecommendedActions())
                .thenReturn("[\"Verify external record\"]");
        when(finding.getLimitations())
                .thenReturn("[\"External system unavailable\"]");
        when(finding.isRequiresHumanReview()).thenReturn(true);
        when(finding.getModelName()).thenReturn("llama3.2:3b");

        LocalDateTime createdAt = LocalDateTime.now();
        when(finding.getCreatedAt()).thenReturn(createdAt);

        when(investigationRepository
                .findByIdAndOrgId(investigationId, orgId))
                .thenReturn(Optional.of(investigation));

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        orgId, investigationId))
                .thenReturn(List.of(finding));

        when(objectMapper.readValue(
                eq(finding.getEvidence()),
                any(TypeReference.class)))
                .thenReturn(List.of("External record missing"));

        when(objectMapper.readValue(
                eq(finding.getRecommendedActions()),
                any(TypeReference.class)))
                .thenReturn(List.of("Verify external record"));

        when(objectMapper.readValue(
                eq(finding.getLimitations()),
                any(TypeReference.class)))
                .thenReturn(List.of("External system unavailable"));

        List<AIInvestigationFindingResponse> results =
                findingService.getFindings(investigationId);

        assertEquals(1, results.size());

        AIInvestigationFindingResponse result = results.get(0);

        assertEquals(1L, result.id());
        assertEquals(investigationId, result.investigationId());
        assertEquals(20L, result.transactionId());

        assertEquals(
                "No matching external payment record found",
                result.likelyCause());

        assertEquals(
                List.of("External record missing"),
                result.evidence());

        assertEquals(
                List.of("Verify external record"),
                result.recommendedActions());

        assertEquals(
                List.of("External system unavailable"),
                result.limitations());

        assertTrue(result.requiresHumanReview());
        assertEquals("llama3.2:3b", result.modelName());
        assertEquals(createdAt, result.createdAt());

        verify(investigationRepository)
                .findByIdAndOrgId(investigationId, orgId);

        verify(findingRepository)
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        orgId, investigationId);
    }

    @Test
    void shouldRejectInvestigationOwnedByAnotherOrganization() {
        Long investigationId = 15L;
        Long currentOrgId = 1L;

        when(investigationRepository
                .findByIdAndOrgId(investigationId, currentOrgId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> findingService.getFindings(investigationId));

        assertEquals(404, exception.getStatusCode().value());

        verify(findingRepository, never())
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        anyLong(), anyLong());
    }

    @Test
    void shouldReturnEmptyListWhenNoFindingsExist() {
        Long orgId = 1L;
        Long investigationId = 14L;

        Investigation investigation = new Investigation();
        investigation.setId(investigationId);
        investigation.setOrgId(orgId);

        when(investigationRepository
                .findByIdAndOrgId(investigationId, orgId))
                .thenReturn(Optional.of(investigation));

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        orgId, investigationId))
                .thenReturn(List.of());

        List<AIInvestigationFindingResponse> results =
                findingService.getFindings(investigationId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldRejectNullInvestigationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.getFindings(null));

        verifyNoInteractions(
                findingRepository,
                investigationRepository);
    }

    @Test
    void shouldRejectRequestWhenTenantContextIsMissing() {
        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> findingService.getFindings(14L));

        verifyNoInteractions(
                findingRepository,
                investigationRepository);
    }
}

