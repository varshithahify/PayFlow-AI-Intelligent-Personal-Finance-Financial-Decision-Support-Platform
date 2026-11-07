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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIInvestigationFindingServiceTest {

    private static final Long ORG_ID = 1L;
    private static final Long TRANSACTION_ID = 100L;
    private static final Long INVESTIGATION_ID = 200L;

    @Mock
    private AIInvestigationFindingRepository findingRepository;

    @Mock
    private InvestigationRepository investigationRepository;

    @Spy
    private ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private AIInvestigationFindingService findingService;

    private Investigation investigation;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(ORG_ID);
        investigation = mock(Investigation.class);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------------------------------------------------------
    // TEST HELPERS
    // ---------------------------------------------------------

    private AIInvestigationResponse validResponse() {

        return new AIInvestigationResponse(
                "Possible gateway inconsistency",
                List.of(
                        "Internal amount differs from external amount"
                ),
                List.of(
                        "Verify gateway transaction records"
                ),
                true,
                List.of(
                        "External gateway data may be delayed"
                )
        );
    }

    private void mockExistingInvestigation() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.of(investigation));

        when(investigation.getTransactionId())
                .thenReturn(TRANSACTION_ID);
    }

    private AIInvestigationFinding createFinding() {

        AIInvestigationFinding finding =
                mock(AIInvestigationFinding.class);

        lenient().when(finding.getId()).thenReturn(10L);

        lenient().when(finding.getOrgId()).thenReturn(ORG_ID);

        lenient().when(finding.getInvestigationId())
                .thenReturn(INVESTIGATION_ID);

        lenient().when(finding.getTransactionId())
                .thenReturn(TRANSACTION_ID);

        lenient().when(finding.getLikelyCause())
                .thenReturn("Possible gateway inconsistency");

        lenient().when(finding.getEvidence()).thenReturn(
                "[\"Internal amount differs from external amount\"]"
        );

        lenient().when(finding.getRecommendedActions()).thenReturn(
                "[\"Verify gateway transaction records\"]"
        );

        lenient().when(finding.getLimitations()).thenReturn(
                "[\"External gateway data may be delayed\"]"
        );

        lenient().when(finding.isRequiresHumanReview())
                .thenReturn(true);

        lenient().when(finding.getModelName())
                .thenReturn("llama3.2:3b");

        lenient().when(finding.getCreatedAt())
                .thenReturn(LocalDateTime.now());

        return finding;
    }

    // ---------------------------------------------------------
    // SAVE FINDING TESTS
    // ---------------------------------------------------------

    @Test
    void shouldSuccessfullySaveFinding() {

        mockExistingInvestigation();

        when(findingRepository.save(any(AIInvestigationFinding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AIInvestigationFinding result =
                findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        validResponse()
                );

        assertNotNull(result);
        assertEquals(ORG_ID, result.getOrgId());
        assertEquals(INVESTIGATION_ID, result.getInvestigationId());
        assertEquals(TRANSACTION_ID, result.getTransactionId());

        assertEquals(
                "Possible gateway inconsistency",
                result.getLikelyCause()
        );

        assertTrue(result.isRequiresHumanReview());
        assertEquals("llama3.2:3b", result.getModelName());

        verify(findingRepository).save(any(AIInvestigationFinding.class));
    }

    @Test
    void shouldSerializeFindingListsCorrectly() throws Exception {

        mockExistingInvestigation();

        when(findingRepository.save(any(AIInvestigationFinding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AIInvestigationFinding result =
                findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        validResponse()
                );

        assertEquals(
                List.of("Internal amount differs from external amount"),
                objectMapper.readValue(
                        result.getEvidence(),
                        new TypeReference<List<String>>() {}
                )
        );

        assertEquals(
                List.of("Verify gateway transaction records"),
                objectMapper.readValue(
                        result.getRecommendedActions(),
                        new TypeReference<List<String>>() {}
                )
        );

        assertEquals(
                List.of("External gateway data may be delayed"),
                objectMapper.readValue(
                        result.getLimitations(),
                        new TypeReference<List<String>>() {}
                )
        );
    }

    @Test
    void shouldRejectNullTransactionId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        null,
                        INVESTIGATION_ID,
                        validResponse()
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectNullInvestigationId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        null,
                        validResponse()
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectNullResponse() {

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        null
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectBlankLikelyCause() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        " ",
                        List.of("Evidence"),
                        List.of("Action"),
                        true,
                        List.of("Limitation")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectNullEvidence() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        null,
                        List.of("Action"),
                        true,
                        List.of("Limitation")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectNullRecommendedActions() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        List.of("Evidence"),
                        null,
                        true,
                        List.of("Limitation")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectNullLimitations() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        List.of("Evidence"),
                        List.of("Action"),
                        true,
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );

        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldRejectBlankEvidence() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        List.of(" "),
                        List.of("Action"),
                        true,
                        List.of("Limitation")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );
    }

    @Test
    void shouldRejectBlankRecommendedAction() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        List.of("Evidence"),
                        List.of(" "),
                        true,
                        List.of("Limitation")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );
    }

    @Test
    void shouldRejectBlankLimitation() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        List.of("Evidence"),
                        List.of("Action"),
                        true,
                        List.of(" ")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );
    }

    @Test
    void shouldRejectDisabledHumanReview() {

        AIInvestigationResponse response =
                new AIInvestigationResponse(
                        "Cause",
                        List.of("Evidence"),
                        List.of("Action"),
                        false,
                        List.of("Limitation")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        response
                )
        );
    }

    @Test
    void shouldRejectInvestigationNotFound() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        validResponse()
                )
        );

        assertEquals(
                "Investigation not found",
                exception.getMessage()
        );

        verify(findingRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvestigationBelongingToAnotherTransaction() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.of(investigation));

        when(investigation.getTransactionId())
                .thenReturn(999L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        validResponse()
                )
        );

        assertEquals(
                "Investigation does not belong to the transaction",
                exception.getMessage()
        );

        verify(findingRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingOrganizationContext() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        validResponse()
                )
        );

        verifyNoInteractions(investigationRepository);
        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldHandleSerializationFailure() throws Exception {

        mockExistingInvestigation();

        doThrow(new JsonProcessingException(
                "Simulated serialization failure") {})
                .when(objectMapper)
                .writeValueAsString(anyList());

        assertThrows(
                IllegalStateException.class,
                () -> findingService.saveFinding(
                        TRANSACTION_ID,
                        INVESTIGATION_ID,
                        validResponse()
                )
        );

        verify(findingRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // GET FINDINGS TESTS
    // ---------------------------------------------------------

    @Test
    void shouldSuccessfullyRetrieveFindings() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.of(investigation));

        AIInvestigationFinding finding = createFinding();

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        ORG_ID,
                        INVESTIGATION_ID
                )).thenReturn(List.of(finding));

        List<AIInvestigationFindingResponse> results =
                findingService.getFindings(INVESTIGATION_ID);

        assertEquals(1, results.size());

        AIInvestigationFindingResponse result = results.get(0);

        assertEquals(10L, result.id());
        assertEquals(INVESTIGATION_ID, result.investigationId());
        assertEquals(TRANSACTION_ID, result.transactionId());

        assertEquals(
                "Possible gateway inconsistency",
                result.likelyCause()
        );

        assertTrue(result.requiresHumanReview());
        assertEquals("llama3.2:3b", result.modelName());

        assertEquals(
                List.of("Internal amount differs from external amount"),
                result.evidence()
        );

        assertEquals(
                List.of("Verify gateway transaction records"),
                result.recommendedActions()
        );

        assertEquals(
                List.of("External gateway data may be delayed"),
                result.limitations()
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoFindingsExist() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.of(investigation));

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        ORG_ID,
                        INVESTIGATION_ID
                )).thenReturn(List.of());

        List<AIInvestigationFindingResponse> results =
                findingService.getFindings(INVESTIGATION_ID);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldRejectNullInvestigationIdDuringRetrieval() {

        assertThrows(
                IllegalArgumentException.class,
                () -> findingService.getFindings(null)
        );

        verifyNoInteractions(investigationRepository);
        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldReturnNotFoundForUnauthorizedInvestigation() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> findingService.getFindings(INVESTIGATION_ID)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(findingRepository, never())
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void shouldRejectMissingOrganizationContextDuringRetrieval() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> findingService.getFindings(INVESTIGATION_ID)
        );

        verifyNoInteractions(investigationRepository);
        verifyNoInteractions(findingRepository);
    }

    @Test
    void shouldHandleMalformedSavedJson() {

        AIInvestigationFinding finding = createFinding();

        when(finding.getEvidence()).thenReturn("invalid-json");

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.of(investigation));

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        ORG_ID,
                        INVESTIGATION_ID
                )).thenReturn(List.of(finding));

        assertThrows(
                IllegalStateException.class,
                () -> findingService.getFindings(INVESTIGATION_ID)
        );
    }

    @Test
    void shouldUseAuthenticatedOrganizationForRetrieval() {

        when(investigationRepository.findByIdAndOrgId(
                INVESTIGATION_ID,
                ORG_ID
        )).thenReturn(Optional.of(investigation));

        when(findingRepository
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        ORG_ID,
                        INVESTIGATION_ID
                )).thenReturn(List.of());

        findingService.getFindings(INVESTIGATION_ID);

        verify(investigationRepository)
                .findByIdAndOrgId(INVESTIGATION_ID, ORG_ID);

        verify(findingRepository)
                .findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        ORG_ID,
                        INVESTIGATION_ID
                );
    }
}

