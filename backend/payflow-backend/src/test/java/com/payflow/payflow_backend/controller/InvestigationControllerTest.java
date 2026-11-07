package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.service.InvestigationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvestigationController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvestigationService investigationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    private Investigation createInvestigation(Long id) {

        Investigation investigation = new Investigation();

        investigation.setId(id);
        investigation.setTransactionId(10L);
        investigation.setOrgId(1L);
        investigation.setReconciliationRecordId(20L);

        investigation.setStatus(InvestigationStatus.OPEN);
        investigation.setPriority(InvestigationPriority.HIGH);
        investigation.setIssueType(
                InvestigationIssueType.AMOUNT_MISMATCH
        );

        investigation.setSummary("Transaction amount mismatch");

        LocalDateTime now = LocalDateTime.now();

        investigation.setCreatedAt(now);
        investigation.setUpdatedAt(now);

        return investigation;
    }


    // --------------------------------------------------
    // CREATE INVESTIGATION TESTS
    // --------------------------------------------------

    @Test
    void shouldCreateInvestigationSuccessfully() throws Exception {

        Investigation investigation = createInvestigation(1L);

        when(investigationService.createInvestigation(
                10L,
                20L,
                InvestigationPriority.HIGH,
                InvestigationIssueType.AMOUNT_MISMATCH,
                "Transaction amount mismatch"
        )).thenReturn(investigation);

        mockMvc.perform(
                post("/api/investigations")
                        .param("transactionId", "10")
                        .param("reconciliationRecordId", "20")
                        .param("priority", "HIGH")
                        .param("issueType", "AMOUNT_MISMATCH")
                        .param("summary", "Transaction amount mismatch")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.transactionId").value(10))
        .andExpect(jsonPath("$.orgId").value(1))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.issueType").value("AMOUNT_MISMATCH"));

        verify(investigationService).createInvestigation(
                10L,
                20L,
                InvestigationPriority.HIGH,
                InvestigationIssueType.AMOUNT_MISMATCH,
                "Transaction amount mismatch"
        );
    }


    @Test
    void shouldCreateInvestigationWithoutReconciliationRecord()
            throws Exception {

        Investigation investigation = createInvestigation(2L);
        investigation.setReconciliationRecordId(null);

        when(investigationService.createInvestigation(
                10L,
                null,
                InvestigationPriority.CRITICAL,
                InvestigationIssueType.PROCESSING_FAILURE,
                "Payment processing failed"
        )).thenReturn(investigation);

        mockMvc.perform(
                post("/api/investigations")
                        .param("transactionId", "10")
                        .param("priority", "CRITICAL")
                        .param("issueType", "PROCESSING_FAILURE")
                        .param("summary", "Payment processing failed")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.reconciliationRecordId").doesNotExist());

        verify(investigationService).createInvestigation(
                10L,
                null,
                InvestigationPriority.CRITICAL,
                InvestigationIssueType.PROCESSING_FAILURE,
                "Payment processing failed"
        );
    }


    @Test
    void shouldRejectCreationWhenTransactionIdIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/api/investigations")
                        .param("priority", "HIGH")
                        .param("issueType", "AMOUNT_MISMATCH")
                        .param("summary", "Amount mismatch")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }


    @Test
    void shouldRejectCreationWhenPriorityIsInvalid()
            throws Exception {

        mockMvc.perform(
                post("/api/investigations")
                        .param("transactionId", "10")
                        .param("priority", "INVALID")
                        .param("issueType", "AMOUNT_MISMATCH")
                        .param("summary", "Amount mismatch")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }


    @Test
    void shouldRejectCreationWhenIssueTypeIsInvalid()
            throws Exception {

        mockMvc.perform(
                post("/api/investigations")
                        .param("transactionId", "10")
                        .param("priority", "HIGH")
                        .param("issueType", "INVALID")
                        .param("summary", "Amount mismatch")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }


    // --------------------------------------------------
    // GET INVESTIGATION BY ID TESTS
    // --------------------------------------------------

    @Test
    void shouldReturnInvestigationById() throws Exception {

        Investigation investigation = createInvestigation(1L);

        when(investigationService.getInvestigation(1L))
                .thenReturn(investigation);

        mockMvc.perform(
                get("/api/investigations/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.transactionId").value(10))
        .andExpect(jsonPath("$.status").value("OPEN"));

        verify(investigationService).getInvestigation(1L);
    }


    // --------------------------------------------------
    // GET INVESTIGATIONS BY STATUS TESTS
    // --------------------------------------------------

    @Test
    void shouldReturnInvestigationsByStatus() throws Exception {

        List<Investigation> investigations = List.of(
                createInvestigation(1L),
                createInvestigation(2L)
        );

        when(investigationService.getByStatus(
                InvestigationStatus.OPEN
        )).thenReturn(investigations);

        mockMvc.perform(
                get("/api/investigations/status/OPEN")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(investigationService).getByStatus(
                InvestigationStatus.OPEN
        );
    }


    @Test
    void shouldReturnEmptyListWhenNoInvestigationsMatchStatus()
            throws Exception {

        when(investigationService.getByStatus(
                InvestigationStatus.RESOLVED
        )).thenReturn(List.of());

        mockMvc.perform(
                get("/api/investigations/status/RESOLVED")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(investigationService).getByStatus(
                InvestigationStatus.RESOLVED
        );
    }


    @Test
    void shouldRejectInvalidInvestigationStatus()
            throws Exception {

        mockMvc.perform(
                get("/api/investigations/status/INVALID")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }


    // --------------------------------------------------
    // GET INVESTIGATIONS BY PRIORITY TESTS
    // --------------------------------------------------

    @Test
    void shouldReturnInvestigationsByPriority()
            throws Exception {

        List<Investigation> investigations = List.of(
                createInvestigation(1L),
                createInvestigation(2L)
        );

        when(investigationService.getByPriority(
                InvestigationPriority.HIGH
        )).thenReturn(investigations);

        mockMvc.perform(
                get("/api/investigations/priority/HIGH")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].priority").value("HIGH"));

        verify(investigationService).getByPriority(
                InvestigationPriority.HIGH
        );
    }


    @Test
    void shouldRejectInvalidInvestigationPriority()
            throws Exception {

        mockMvc.perform(
                get("/api/investigations/priority/INVALID")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }


    // --------------------------------------------------
    // GET INVESTIGATIONS BY TRANSACTION ID TESTS
    // --------------------------------------------------

    @Test
    void shouldReturnInvestigationsByTransactionId()
            throws Exception {

        List<Investigation> investigations = List.of(
                createInvestigation(1L),
                createInvestigation(2L)
        );

        when(investigationService.getByTransactionId(10L))
                .thenReturn(investigations);

        mockMvc.perform(
                get("/api/investigations/transaction/10")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].transactionId").value(10));

        verify(investigationService).getByTransactionId(10L);
    }


    @Test
    void shouldReturnEmptyListWhenTransactionHasNoInvestigations()
            throws Exception {

        when(investigationService.getByTransactionId(99L))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/investigations/transaction/99")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(investigationService).getByTransactionId(99L);
    }


    // --------------------------------------------------
    // UPDATE INVESTIGATION STATUS TESTS
    // --------------------------------------------------

    @Test
    void shouldUpdateInvestigationStatusSuccessfully()
            throws Exception {

        Investigation investigation = createInvestigation(1L);
        investigation.setStatus(InvestigationStatus.INVESTIGATING);

        when(investigationService.updateStatus(
                1L,
                InvestigationStatus.INVESTIGATING
        )).thenReturn(investigation);

        mockMvc.perform(
                post("/api/investigations/1/status")
                        .param("status", "INVESTIGATING")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("INVESTIGATING"));

        verify(investigationService).updateStatus(
                1L,
                InvestigationStatus.INVESTIGATING
        );
    }


    @Test
    void shouldRejectStatusUpdateWhenStatusIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/api/investigations/1/status")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }


    @Test
    void shouldRejectStatusUpdateWhenStatusIsInvalid()
            throws Exception {

        mockMvc.perform(
                post("/api/investigations/1/status")
                        .param("status", "INVALID")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(investigationService);
    }
}