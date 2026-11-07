package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.security.TenantContext;
import com.payflow.payflow_backend.service.ReconciliationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReconciliationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }


    private ReconciliationRecord createRecord(Long id) {

        ReconciliationRecord record = new ReconciliationRecord();

        record.setId(id);
        record.setTransactionId(10L);
        record.setOrgId(1L);
        record.setExternalRecordId(20L);

        record.setStatus(ReconciliationStatus.MATCHED);

        record.setInternalAmount(new BigDecimal("100.00"));
        record.setExternalAmount(new BigDecimal("100.00"));

        record.setInternalStatus("SUCCESS");
        record.setExternalStatus("SUCCESS");

        record.setMismatchReason(null);
        record.setReconciledAt(LocalDateTime.now());

        return record;
    }


    // --------------------------------------------------
    // RECONCILE INTERNAL TRANSACTION
    // --------------------------------------------------

    @Test
    void shouldReconcileTransactionSuccessfully() throws Exception {

        ReconciliationRecord record = createRecord(1L);

        when(reconciliationService.reconcileTransaction(10L, 1L))
                .thenReturn(record);

        mockMvc.perform(
                post("/api/reconciliation/transactions/10")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.transactionId").value(10))
        .andExpect(jsonPath("$.orgId").value(1))
        .andExpect(jsonPath("$.status").value("MATCHED"))
        .andExpect(jsonPath("$.internalAmount").value(100.00))
        .andExpect(jsonPath("$.externalAmount").value(100.00));

        verify(reconciliationService)
                .reconcileTransaction(10L, 1L);
    }


    // --------------------------------------------------
    // RECONCILE EXTERNAL PAYMENT RECORD
    // --------------------------------------------------

    @Test
    void shouldReconcileExternalRecordSuccessfully()
            throws Exception {

        ReconciliationRecord record = createRecord(2L);

        when(reconciliationService.reconcileExternalRecord(20L, 1L))
                .thenReturn(record);

        mockMvc.perform(
                post("/api/reconciliation/external-records/20")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.externalRecordId").value(20))
        .andExpect(jsonPath("$.status").value("MATCHED"));

        verify(reconciliationService)
                .reconcileExternalRecord(20L, 1L);
    }


    // --------------------------------------------------
    // GET LATEST RECONCILIATION
    // --------------------------------------------------

    @Test
    void shouldReturnLatestReconciliationSuccessfully()
            throws Exception {

        ReconciliationRecord record = createRecord(1L);

        when(reconciliationService.getLatestReconciliation(10L))
                .thenReturn(Optional.of(record));

        mockMvc.perform(
                get("/api/reconciliation/transactions/10")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.transactionId").value(10))
        .andExpect(jsonPath("$.status").value("MATCHED"));

        verify(reconciliationService)
                .getLatestReconciliation(10L);
    }


    @Test
    void shouldReturnNotFoundWhenReconciliationDoesNotExist()
            throws Exception {

        when(reconciliationService.getLatestReconciliation(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/api/reconciliation/transactions/99")
        )
        .andExpect(status().isNotFound());

        verify(reconciliationService)
                .getLatestReconciliation(99L);
    }


    // --------------------------------------------------
    // GET RECONCILIATIONS BY STATUS
    // --------------------------------------------------

    @Test
    void shouldReturnReconciliationsByStatus()
            throws Exception {

        List<ReconciliationRecord> records = List.of(
                createRecord(1L),
                createRecord(2L)
        );

        when(reconciliationService.getByStatus(
                ReconciliationStatus.MATCHED
        )).thenReturn(records);

        mockMvc.perform(
                get("/api/reconciliation/status/MATCHED")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].status").value("MATCHED"))
        .andExpect(jsonPath("$[1].status").value("MATCHED"));

        verify(reconciliationService)
                .getByStatus(ReconciliationStatus.MATCHED);
    }


    @Test
    void shouldReturnEmptyListWhenNoRecordsMatchStatus()
            throws Exception {

        when(reconciliationService.getByStatus(
                ReconciliationStatus.MISMATCH
        )).thenReturn(List.of());

        mockMvc.perform(
                get("/api/reconciliation/status/MISMATCH")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(reconciliationService)
                .getByStatus(ReconciliationStatus.MISMATCH);
    }


    @Test
    void shouldRejectInvalidReconciliationStatus()
            throws Exception {

        mockMvc.perform(
                get("/api/reconciliation/status/INVALID")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationService);
    }
}