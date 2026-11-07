package com.payflow.payflow_backend.ai;

import com.payflow.payflow_backend.ai.dto.AIInvestigationContext;
import com.payflow.payflow_backend.entity.*;
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

    private static final Long TRANSACTION_ID = 101L;
    private static final Long ORG_ID = 10L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ReconciliationRecordRepository reconciliationRecordRepository;

    @Mock
    private InvestigationRepository investigationRepository;

    @InjectMocks
    private AIInvestigationContextService contextService;

    private Transaction transaction;

    @BeforeEach
    void setUp() {

        TenantContext.setOrgId(ORG_ID);

        transaction = new Transaction(
                new BigDecimal("1500.00"),
                "INR",
                "UPI",
                TransactionStatus.values()[0],
                20L,
                ORG_ID,
                "idempotency-101"
        );

        transaction.setId(TRANSACTION_ID);
        transaction.setGatewayName("RAZORPAY");

        transaction.setCreatedAt(
                LocalDateTime.of(2026, 11, 4, 10, 30)
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void mockTransaction() {

        when(transactionRepository.findByIdAndOrgId(
                TRANSACTION_ID,
                ORG_ID
        )).thenReturn(Optional.of(transaction));
    }

    private ReconciliationRecord createReconciliation() {

        ReconciliationRecord record = new ReconciliationRecord();

        record.setId(501L);
        record.setTransactionId(TRANSACTION_ID);
        record.setOrgId(ORG_ID);
        record.setStatus(ReconciliationStatus.values()[0]);
        record.setInternalAmount(new BigDecimal("1500.00"));
        record.setExternalAmount(new BigDecimal("1400.00"));
        record.setInternalStatus("SUCCESS");
        record.setExternalStatus("FAILED");
        record.setMismatchReason("Amount mismatch");

        record.setReconciledAt(
                LocalDateTime.of(2026, 11, 4, 11, 0)
        );

        return record;
    }

    private Investigation createInvestigation() {

        Investigation investigation = new Investigation();

        investigation.setId(701L);
        investigation.setTransactionId(TRANSACTION_ID);
        investigation.setOrgId(ORG_ID);
        investigation.setStatus(InvestigationStatus.values()[0]);
        investigation.setPriority(InvestigationPriority.values()[0]);
        investigation.setIssueType(InvestigationIssueType.values()[0]);
        investigation.setSummary("Transaction amount mismatch");

        investigation.setCreatedAt(
                LocalDateTime.of(2026, 11, 4, 11, 30)
        );

        return investigation;
    }

    @Test
    void shouldSuccessfullyBuildCompleteContext() {

        mockTransaction();

        ReconciliationRecord reconciliation = createReconciliation();
        Investigation investigation = createInvestigation();

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID,
                        TRANSACTION_ID
                )).thenReturn(Optional.of(reconciliation));

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID,
                        TRANSACTION_ID
                )).thenReturn(Optional.of(investigation));

        AIInvestigationContext context =
                contextService.buildContext(TRANSACTION_ID);

        assertNotNull(context);

        // Transaction information
        assertEquals(TRANSACTION_ID, context.transactionId());
        assertEquals(new BigDecimal("1500.00"), context.transactionAmount());
        assertEquals("INR", context.transactionCurrency());
        assertEquals("UPI", context.paymentMethod());
        assertEquals(TransactionStatus.values()[0].name(),
                context.transactionStatus());
        assertEquals("RAZORPAY", context.gatewayName());
        assertEquals(transaction.getCreatedAt(),
                context.transactionCreatedAt());

        // Reconciliation information
        assertEquals(501L, context.reconciliationRecordId());
        assertEquals(ReconciliationStatus.values()[0].name(),
                context.reconciliationStatus());
        assertEquals(new BigDecimal("1500.00"), context.internalAmount());
        assertEquals(new BigDecimal("1400.00"), context.externalAmount());
        assertEquals("SUCCESS", context.internalStatus());
        assertEquals("FAILED", context.externalStatus());
        assertEquals("Amount mismatch", context.mismatchReason());
        assertEquals(reconciliation.getReconciledAt(),
                context.reconciledAt());

        // Investigation information
        assertEquals(701L, context.investigationId());
        assertEquals(InvestigationIssueType.values()[0].name(),
                context.issueType());
        assertEquals(InvestigationPriority.values()[0].name(),
                context.investigationPriority());
        assertEquals(InvestigationStatus.values()[0].name(),
                context.investigationStatus());
        assertEquals("Transaction amount mismatch",
                context.existingSummary());

        verify(transactionRepository)
                .findByIdAndOrgId(TRANSACTION_ID, ORG_ID);

        verify(reconciliationRecordRepository)
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID, TRANSACTION_ID);

        verify(investigationRepository)
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID, TRANSACTION_ID);
    }

    @Test
    void shouldBuildContextWhenReconciliationDoesNotExist() {

        mockTransaction();

        Investigation investigation = createInvestigation();

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.empty());

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.of(investigation));

        AIInvestigationContext context =
                contextService.buildContext(TRANSACTION_ID);

        assertNotNull(context);

        assertNull(context.reconciliationRecordId());
        assertNull(context.reconciliationStatus());
        assertNull(context.internalAmount());
        assertNull(context.externalAmount());
        assertNull(context.internalStatus());
        assertNull(context.externalStatus());
        assertNull(context.mismatchReason());
        assertNull(context.reconciledAt());

        assertEquals(701L, context.investigationId());
    }

    @Test
    void shouldBuildContextWhenInvestigationDoesNotExist() {

        mockTransaction();

        ReconciliationRecord reconciliation = createReconciliation();

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.of(reconciliation));

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.empty());

        AIInvestigationContext context =
                contextService.buildContext(TRANSACTION_ID);

        assertNotNull(context);

        assertEquals(501L, context.reconciliationRecordId());

        assertNull(context.investigationId());
        assertNull(context.issueType());
        assertNull(context.investigationPriority());
        assertNull(context.investigationStatus());
        assertNull(context.existingSummary());
    }

    @Test
    void shouldBuildContextWhenOptionalRecordsDoNotExist() {

        mockTransaction();

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.empty());

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.empty());

        AIInvestigationContext context =
                contextService.buildContext(TRANSACTION_ID);

        assertNotNull(context);

        assertEquals(TRANSACTION_ID, context.transactionId());

        assertNull(context.reconciliationRecordId());
        assertNull(context.reconciliationStatus());
        assertNull(context.internalAmount());
        assertNull(context.externalAmount());
        assertNull(context.internalStatus());
        assertNull(context.externalStatus());
        assertNull(context.mismatchReason());
        assertNull(context.reconciledAt());

        assertNull(context.investigationId());
        assertNull(context.issueType());
        assertNull(context.investigationPriority());
        assertNull(context.investigationStatus());
        assertNull(context.existingSummary());
    }

    @Test
    void shouldRejectNullTransactionId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> contextService.buildContext(null)
        );

        verifyNoInteractions(
                transactionRepository,
                reconciliationRecordRepository,
                investigationRepository
        );
    }

    @Test
    void shouldRejectMissingOrganizationContext() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> contextService.buildContext(TRANSACTION_ID)
        );

        verifyNoInteractions(
                transactionRepository,
                reconciliationRecordRepository,
                investigationRepository
        );
    }

    @Test
    void shouldRejectTransactionNotFoundForOrganization() {

        when(transactionRepository.findByIdAndOrgId(
                TRANSACTION_ID,
                ORG_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> contextService.buildContext(TRANSACTION_ID)
        );

        verify(transactionRepository)
                .findByIdAndOrgId(TRANSACTION_ID, ORG_ID);

        verifyNoInteractions(
                reconciliationRecordRepository,
                investigationRepository
        );
    }

    @Test
    void shouldUseAuthenticatedOrganizationIdForAllQueries() {

        mockTransaction();

        when(reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.empty());

        when(investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID, TRANSACTION_ID
                )).thenReturn(Optional.empty());

        contextService.buildContext(TRANSACTION_ID);

        verify(transactionRepository)
                .findByIdAndOrgId(TRANSACTION_ID, ORG_ID);

        verify(reconciliationRecordRepository)
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        ORG_ID, TRANSACTION_ID);

        verify(investigationRepository)
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        ORG_ID, TRANSACTION_ID);
    }
}