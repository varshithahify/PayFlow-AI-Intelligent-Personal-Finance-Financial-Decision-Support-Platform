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
import com.payflow.payflow_backend.entity.InvestigationStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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

    private Investigation createInvestigation(
        InvestigationStatus status) {

    Investigation investigation = new Investigation();

    investigation.setId(10L);
    investigation.setTransactionId(100L);
    investigation.setOrgId(1L);
    investigation.setStatus(status);
    investigation.setPriority(InvestigationPriority.HIGH);
    investigation.setIssueType(InvestigationIssueType.AMOUNT_MISMATCH);
    investigation.setSummary("Amount mismatch");

    return investigation;
}

@Test
void shouldCreateInvestigationUsingTenantContext() {

    Transaction transaction = new Transaction();
    transaction.setId(100L);
    transaction.setOrgId(1L);

    when(transactionRepository.findByIdAndOrgId(100L, 1L))
            .thenReturn(Optional.of(transaction));

    when(investigationRepository.save(any(Investigation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Investigation result = investigationService.createInvestigation(
            100L,
            null,
            InvestigationPriority.HIGH,
            InvestigationIssueType.AMOUNT_MISMATCH,
            "Amount mismatch");

    assertEquals(100L, result.getTransactionId());
    assertEquals(1L, result.getOrgId());
    assertEquals(InvestigationStatus.OPEN, result.getStatus());
    assertNotNull(result.getCreatedAt());
    assertNotNull(result.getUpdatedAt());

    verify(investigationRepository).save(any(Investigation.class));
}

@Test
void shouldRejectNullTransactionId() {

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    null,
                    1L,
                    null,
                    InvestigationPriority.HIGH,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));

    verifyNoInteractions(transactionRepository);
}

@Test
void shouldRejectNullOrganizationId() {

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    null,
                    null,
                    InvestigationPriority.HIGH,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));
}

@Test
void shouldRejectNullPriority() {

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    1L,
                    null,
                    null,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));
}

@Test
void shouldRejectNullIssueType() {

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    1L,
                    null,
                    InvestigationPriority.HIGH,
                    null,
                    "Test"));
}

@Test
void shouldRejectMismatchedTenantOrganization() {

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    2L,
                    null,
                    InvestigationPriority.HIGH,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));

    verifyNoInteractions(transactionRepository);
}

@Test
void shouldRejectTransactionNotFound() {

    when(transactionRepository.findByIdAndOrgId(100L, 1L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    1L,
                    null,
                    InvestigationPriority.HIGH,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));

    verify(investigationRepository, never())
            .save(any(Investigation.class));
}

@Test
void shouldRejectReconciliationRecordWithMismatchedOrganization() {

    Transaction transaction = new Transaction();
    transaction.setId(100L);
    transaction.setOrgId(1L);

    ReconciliationRecord record = new ReconciliationRecord();
    record.setId(500L);
    record.setOrgId(2L);
    record.setTransactionId(100L);

    when(transactionRepository.findByIdAndOrgId(100L, 1L))
            .thenReturn(Optional.of(transaction));

    when(reconciliationRecordRepository
            .findByIdAndOrgIdAndTransactionId(500L, 1L, 100L))
            .thenReturn(Optional.of(record));

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    1L,
                    500L,
                    InvestigationPriority.HIGH,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));

    verify(investigationRepository, never())
            .save(any(Investigation.class));
}

@Test
void shouldRejectReconciliationRecordWithMismatchedTransaction() {

    Transaction transaction = new Transaction();
    transaction.setId(100L);
    transaction.setOrgId(1L);

    ReconciliationRecord record = new ReconciliationRecord();
    record.setId(500L);
    record.setOrgId(1L);
    record.setTransactionId(200L);

    when(transactionRepository.findByIdAndOrgId(100L, 1L))
            .thenReturn(Optional.of(transaction));

    when(reconciliationRecordRepository
            .findByIdAndOrgIdAndTransactionId(500L, 1L, 100L))
            .thenReturn(Optional.of(record));

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.createInvestigation(
                    100L,
                    1L,
                    500L,
                    InvestigationPriority.HIGH,
                    InvestigationIssueType.AMOUNT_MISMATCH,
                    "Test"));
}

@Test
void shouldGetInvestigationSuccessfully() {

    Investigation investigation =
            createInvestigation(InvestigationStatus.OPEN);

    when(investigationRepository.findByIdAndOrgId(10L, 1L))
            .thenReturn(Optional.of(investigation));

    Investigation result =
            investigationService.getInvestigation(10L);

    assertEquals(10L, result.getId());
    assertEquals(1L, result.getOrgId());
}

@Test
void shouldRejectInvestigationNotFound() {

    when(investigationRepository.findByIdAndOrgId(10L, 1L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.getInvestigation(10L));
}

@Test
void shouldGetInvestigationsByStatus() {

    Investigation investigation =
            createInvestigation(InvestigationStatus.OPEN);

    when(investigationRepository
            .findByOrgIdAndStatusOrderByCreatedAtDesc(
                    1L,
                    InvestigationStatus.OPEN))
            .thenReturn(List.of(investigation));

    List<Investigation> result =
            investigationService.getByStatus(InvestigationStatus.OPEN);

    assertEquals(1, result.size());
    assertEquals(InvestigationStatus.OPEN, result.get(0).getStatus());
}

@Test
void shouldGetInvestigationsByPriority() {

    Investigation investigation =
            createInvestigation(InvestigationStatus.OPEN);

    when(investigationRepository
            .findByOrgIdAndPriorityOrderByCreatedAtDesc(
                    1L,
                    InvestigationPriority.HIGH))
            .thenReturn(List.of(investigation));

    List<Investigation> result =
            investigationService.getByPriority(
                    InvestigationPriority.HIGH);

    assertEquals(1, result.size());
    assertEquals(InvestigationPriority.HIGH, result.get(0).getPriority());
}

@Test
void shouldGetInvestigationsByTransactionId() {

    Transaction transaction = new Transaction();
    transaction.setId(100L);
    transaction.setOrgId(1L);

    Investigation investigation =
            createInvestigation(InvestigationStatus.OPEN);

    when(transactionRepository.findByIdAndOrgId(100L, 1L))
            .thenReturn(Optional.of(transaction));

    when(investigationRepository
            .findByOrgIdAndTransactionIdOrderByCreatedAtDesc(1L, 100L))
            .thenReturn(List.of(investigation));

    List<Investigation> result =
            investigationService.getByTransactionId(100L);

    assertEquals(1, result.size());

    verify(transactionRepository).findByIdAndOrgId(100L, 1L);
}

@Test
void shouldRejectTransactionNotOwnedWhenGettingInvestigations() {

    when(transactionRepository.findByIdAndOrgId(100L, 1L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.getByTransactionId(100L));

    verify(investigationRepository, never())
            .findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                    anyLong(),
                    anyLong());
}

@Test
void shouldRejectNullNewStatus() {

    assertThrows(
            IllegalArgumentException.class,
            () -> investigationService.updateStatus(10L, null));

    verify(investigationRepository, never()).save(any());
}

@Test
void shouldUpdateOpenToInvestigating() {

    verifyStatusTransition(
            InvestigationStatus.OPEN,
            InvestigationStatus.INVESTIGATING);
}

@Test
void shouldUpdateOpenToEscalated() {

    verifyStatusTransition(
            InvestigationStatus.OPEN,
            InvestigationStatus.ESCALATED);
}

@Test
void shouldUpdateInvestigatingToResolved() {

    Investigation result = verifyStatusTransition(
            InvestigationStatus.INVESTIGATING,
            InvestigationStatus.RESOLVED);

    assertNotNull(result.getResolvedAt());
}

@Test
void shouldUpdateInvestigatingToEscalated() {

    verifyStatusTransition(
            InvestigationStatus.INVESTIGATING,
            InvestigationStatus.ESCALATED);
}

@Test
void shouldUpdateEscalatedToInvestigating() {

    verifyStatusTransition(
            InvestigationStatus.ESCALATED,
            InvestigationStatus.INVESTIGATING);
}

@Test
void shouldUpdateEscalatedToResolved() {

    Investigation result = verifyStatusTransition(
            InvestigationStatus.ESCALATED,
            InvestigationStatus.RESOLVED);

    assertNotNull(result.getResolvedAt());
}

@Test
void shouldRejectTransitionFromResolved() {

    assertInvalidTransition(
            InvestigationStatus.RESOLVED,
            InvestigationStatus.OPEN);
}

@Test
void shouldRejectSameStatusTransition() {

    assertInvalidTransition(
            InvestigationStatus.OPEN,
            InvestigationStatus.OPEN);
}

@Test
void shouldRejectInvalidOpenToResolvedTransition() {

    assertInvalidTransition(
            InvestigationStatus.OPEN,
            InvestigationStatus.RESOLVED);
}

@Test
void shouldRejectInvalidEscalatedToOpenTransition() {

    assertInvalidTransition(
            InvestigationStatus.ESCALATED,
            InvestigationStatus.OPEN);
}

private Investigation verifyStatusTransition(
        InvestigationStatus currentStatus,
        InvestigationStatus newStatus) {

    Investigation investigation =
            createInvestigation(currentStatus);

    when(investigationRepository.findByIdAndOrgId(10L, 1L))
            .thenReturn(Optional.of(investigation));

    when(investigationRepository.save(any(Investigation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Investigation result =
            investigationService.updateStatus(10L, newStatus);

    assertEquals(newStatus, result.getStatus());
    assertNotNull(result.getUpdatedAt());

    verify(investigationRepository).save(investigation);

    return result;
}

private void assertInvalidTransition(
        InvestigationStatus currentStatus,
        InvestigationStatus newStatus) {

    Investigation investigation =
            createInvestigation(currentStatus);

    when(investigationRepository.findByIdAndOrgId(10L, 1L))
            .thenReturn(Optional.of(investigation));

    assertThrows(
            IllegalStateException.class,
            () -> investigationService.updateStatus(10L, newStatus));

    verify(investigationRepository, never())
            .save(any(Investigation.class));
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

