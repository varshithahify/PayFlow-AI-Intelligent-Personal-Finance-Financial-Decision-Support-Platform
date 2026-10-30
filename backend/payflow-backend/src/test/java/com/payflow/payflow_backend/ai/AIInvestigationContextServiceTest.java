package com.payflow.payflow_backend.ai;

import com.payflow.payflow_backend.ai.dto.AIInvestigationContext;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIInvestigationContextServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ReconciliationRecordRepository reconciliationRecordRepository;

    @Mock
    private InvestigationRepository investigationRepository;

    @InjectMocks
    private AIInvestigationContextService contextService;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldBuildContextFromAuthorizedTransactionAndEvidence() {

        Long transactionId = 7L;
        Long orgId = 1L;

        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(orgId);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setCurrency("INR");
        transaction.setPaymentMethod("CARD");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setGatewayName("GATEWAY_A");
        transaction.setCreatedAt(now);

        ReconciliationRecord reconciliation = new ReconciliationRecord();
        reconciliation.setId(100L);
        reconciliation.setOrgId(orgId);
        reconciliation.setTransactionId(transactionId);
        reconciliation.setStatus(ReconciliationStatus.MISMATCH);
        reconciliation.setInternalAmount(new BigDecimal("1000.00"));
        reconciliation.setExternalAmount(new BigDecimal("900.00"));
        reconciliation.setInternalStatus("SUCCESS");
        reconciliation.setExternalStatus("PENDING");
        reconciliation.setMismatchReason("Amount mismatch.");
        reconciliation.setReconciledAt(now);

        Investigation investigation = new Investigation();
        investigation.setId(200L);
        investigation.setOrgId(orgId);
        investigation.setTransactionId(transactionId);
        investigation.setIssueType(InvestigationIssueType.AMOUNT_MISMATCH);
        investigation.setPriority(InvestigationPriority.HIGH);
        investigation.setStatus(InvestigationStatus.OPEN);
        investigation.setSummary("Amount mismatch.");

        when(transactionRepository.findByIdAndOrgId(transactionId, orgId))
                .thenReturn(Optional.of(transaction));

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        orgId, transactionId))
                .thenReturn(Optional.of(reconciliation));

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        orgId, transactionId))
                .thenReturn(Optional.of(investigation));

        AIInvestigationContext context =
                contextService.buildContext(transactionId);

        assertEquals(transactionId, context.transactionId());
        assertEquals(new BigDecimal("1000.00"), context.transactionAmount());
        assertEquals("INR", context.transactionCurrency());
        assertEquals("SUCCESS", context.transactionStatus());

        assertEquals(100L, context.reconciliationRecordId());
        assertEquals("MISMATCH", context.reconciliationStatus());
        assertEquals(new BigDecimal("900.00"), context.externalAmount());
        assertEquals("Amount mismatch.", context.mismatchReason());

        assertEquals(200L, context.investigationId());
        assertEquals("AMOUNT_MISMATCH", context.issueType());
        assertEquals("HIGH", context.investigationPriority());
        assertEquals("OPEN", context.investigationStatus());

        verify(transactionRepository)
                .findByIdAndOrgId(transactionId, orgId);
    }

    @Test
    void shouldRejectTransactionNotOwnedByCurrentOrganization() {

        Long transactionId = 7L;
        Long authenticatedOrgId = 1L;

        when(transactionRepository.findByIdAndOrgId(
                transactionId, authenticatedOrgId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> contextService.buildContext(transactionId));

        verify(transactionRepository)
                .findByIdAndOrgId(transactionId, authenticatedOrgId);

        verifyNoInteractions(
                reconciliationRecordRepository,
                investigationRepository);
    }

    @Test
    void shouldUseAuthenticatedOrganizationIdForTransactionLookup() {

        Long transactionId = 7L;
        Long authenticatedOrgId = 2L;

        TenantContext.setOrgId(authenticatedOrgId);

        when(transactionRepository.findByIdAndOrgId(
                transactionId, authenticatedOrgId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> contextService.buildContext(transactionId));

        verify(transactionRepository)
                .findByIdAndOrgId(transactionId, authenticatedOrgId);

        verify(transactionRepository, never())
                .findByIdAndOrgId(transactionId, 1L);

        verifyNoInteractions(
                reconciliationRecordRepository,
                investigationRepository);
    }

    @Test
    void shouldBuildContextWhenReconciliationAndInvestigationAreMissing() {

        Long transactionId = 7L;
        Long orgId = 1L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(orgId);
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setCurrency("INR");
        transaction.setPaymentMethod("UPI");
        transaction.setStatus(TransactionStatus.SUCCESS);

        when(transactionRepository.findByIdAndOrgId(transactionId, orgId))
                .thenReturn(Optional.of(transaction));

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        orgId, transactionId))
                .thenReturn(Optional.empty());

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        orgId, transactionId))
                .thenReturn(Optional.empty());

        AIInvestigationContext context =
                contextService.buildContext(transactionId);

        assertEquals(transactionId, context.transactionId());
        assertNull(context.reconciliationRecordId());
        assertNull(context.reconciliationStatus());
        assertNull(context.externalAmount());
        assertNull(context.investigationId());
        assertNull(context.issueType());
    }

    @Test
    void shouldRejectNullTransactionId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> contextService.buildContext(null));

        verifyNoInteractions(
                transactionRepository,
                reconciliationRecordRepository,
                investigationRepository);
    }

    @Test
    void shouldRejectRequestWhenTenantContextIsMissing() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> contextService.buildContext(7L));

        verifyNoInteractions(
                transactionRepository,
                reconciliationRecordRepository,
                investigationRepository);
    }
}

