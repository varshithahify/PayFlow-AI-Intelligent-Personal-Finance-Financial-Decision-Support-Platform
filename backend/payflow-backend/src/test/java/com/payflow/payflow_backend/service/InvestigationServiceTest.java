package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvestigationServiceTest {

    @Mock
    private InvestigationRepository investigationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ReconciliationRecordRepository reconciliationRecordRepository;

    @InjectMocks
    private InvestigationService investigationService;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateInvestigationWithValidReconciliationRecord() {

        Long transactionId = 100L;
        Long orgId = 1L;
        Long reconciliationId = 500L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(orgId);

        ReconciliationRecord record = new ReconciliationRecord();
        record.setId(reconciliationId);
        record.setOrgId(orgId);
        record.setTransactionId(transactionId);

        when(transactionRepository.findByIdAndOrgId(transactionId, orgId))
                .thenReturn(Optional.of(transaction));

        when(reconciliationRecordRepository
                .findByIdAndOrgIdAndTransactionId(
                        reconciliationId, orgId, transactionId))
                .thenReturn(Optional.of(record));

        when(investigationRepository.save(any(Investigation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Investigation result = investigationService.createInvestigation(
                transactionId,
                orgId,
                reconciliationId,
                InvestigationPriority.HIGH,
                InvestigationIssueType.AMOUNT_MISMATCH,
                "Amount mismatch");

        assertEquals(transactionId, result.getTransactionId());
        assertEquals(orgId, result.getOrgId());
        assertEquals(reconciliationId, result.getReconciliationRecordId());
        assertEquals("Amount mismatch", result.getSummary());

        verify(transactionRepository)
                .findByIdAndOrgId(transactionId, orgId);

        verify(reconciliationRecordRepository)
                .findByIdAndOrgIdAndTransactionId(
                        reconciliationId, orgId, transactionId);

        verify(investigationRepository).save(any(Investigation.class));
    }

    @Test
    void shouldRejectReconciliationRecordNotOwnedByOrganizationOrTransaction() {

        Long transactionId = 100L;
        Long orgId = 1L;
        Long reconciliationId = 500L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(orgId);

        when(transactionRepository.findByIdAndOrgId(transactionId, orgId))
                .thenReturn(Optional.of(transaction));

        when(reconciliationRecordRepository
                .findByIdAndOrgIdAndTransactionId(
                        reconciliationId, orgId, transactionId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> investigationService.createInvestigation(
                        transactionId,
                        orgId,
                        reconciliationId,
                        InvestigationPriority.HIGH,
                        InvestigationIssueType.AMOUNT_MISMATCH,
                        "Amount mismatch"));

        verify(investigationRepository, never())
                .save(any(Investigation.class));
    }

    @Test
    void shouldAllowInvestigationWithoutOptionalReconciliationRecord() {

        Long transactionId = 100L;
        Long orgId = 1L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(orgId);

        when(transactionRepository.findByIdAndOrgId(transactionId, orgId))
                .thenReturn(Optional.of(transaction));

        when(investigationRepository.save(any(Investigation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Investigation result = investigationService.createInvestigation(
                transactionId,
                orgId,
                null,
                InvestigationPriority.HIGH,
                InvestigationIssueType.MISSING_EXTERNAL,
                "External record missing");

        assertEquals(transactionId, result.getTransactionId());
        assertEquals(orgId, result.getOrgId());
        assertNull(result.getReconciliationRecordId());
        assertEquals("External record missing", result.getSummary());

        verifyNoInteractions(reconciliationRecordRepository);
        verify(investigationRepository).save(any(Investigation.class));
    }
}

