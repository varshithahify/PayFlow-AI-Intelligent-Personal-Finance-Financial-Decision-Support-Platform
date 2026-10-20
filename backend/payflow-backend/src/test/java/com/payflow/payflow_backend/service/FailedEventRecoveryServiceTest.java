package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.event.TransactionEvent;
import com.payflow.payflow_backend.repository.FailedEventRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedEventRecoveryServiceTest {

    @Mock
    private FailedEventRecordRepository failedEventRecordRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvestigationService investigationService;

    @InjectMocks
    private FailedEventRecoveryService failedEventRecoveryService;

    @Test
    void shouldRecordFailedEventAndCreateCriticalInvestigation() {
        UUID eventId = UUID.randomUUID();
        Long transactionId = 7L;
        Long orgId = 1L;

        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getTransactionId()).thenReturn(transactionId);
        when(event.getEventType()).thenReturn(null);

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(orgId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(failedEventRecordRepository.insertIfNotExists(
                eq(eventId),
                eq(transactionId),
                eq(orgId),
                eq("UNKNOWN"),
                any(),
                any()))
                .thenReturn(1);

        boolean result = failedEventRecoveryService.recordFailedEvent(
                event,
                "Retries exhausted");

        assertTrue(result);

        verify(failedEventRecordRepository).insertIfNotExists(
                eq(eventId),
                eq(transactionId),
                eq(orgId),
                eq("UNKNOWN"),
                eq("Retries exhausted"),
                any());

        verify(investigationService).createInvestigation(
                eq(transactionId),
                eq(orgId),
                isNull(),
                eq(InvestigationPriority.CRITICAL),
                eq(InvestigationIssueType.PROCESSING_FAILURE),
                contains(eventId.toString()));
    }

    @Test
    void shouldNotCreateDuplicateInvestigationForDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        Long transactionId = 7L;

        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getTransactionId()).thenReturn(transactionId);

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setOrgId(1L);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(failedEventRecordRepository.insertIfNotExists(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        boolean result = failedEventRecoveryService.recordFailedEvent(
                event,
                "Retries exhausted");

        assertFalse(result);

        verify(investigationService, never()).createInvestigation(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void shouldRecordFailureWithoutTenantWhenTransactionDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        Long transactionId = 999L;

        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getTransactionId()).thenReturn(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        when(failedEventRecordRepository.insertIfNotExists(
                eq(eventId),
                isNull(),
                isNull(),
                anyString(),
                any(),
                any()))
                .thenReturn(1);

        boolean result = failedEventRecoveryService.recordFailedEvent(
                event,
                "Transaction not found");

        assertTrue(result);

        verify(failedEventRecordRepository).insertIfNotExists(
                eq(eventId),
                isNull(),
                isNull(),
                anyString(),
                eq("Transaction not found"),
                any());

        verify(investigationService, never()).createInvestigation(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void shouldRejectNullEvent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> failedEventRecoveryService.recordFailedEvent(
                        null,
                        "Retries exhausted"));

        verifyNoInteractions(
                failedEventRecordRepository,
                transactionRepository,
                investigationService);
    }

    @Test
    void shouldRejectEventWithoutId() {
        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getEventId()).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> failedEventRecoveryService.recordFailedEvent(
                        event,
                        "Retries exhausted"));

        verifyNoInteractions(
                failedEventRecordRepository,
                transactionRepository,
                investigationService);
    }

    @Test
void shouldPropagateFailureWhenInvestigationCreationFails() {
    UUID eventId = UUID.randomUUID();
    Long transactionId = 7L;
    Long orgId = 1L;

    TransactionEvent event = mock(TransactionEvent.class);
    when(event.getEventId()).thenReturn(eventId);
    when(event.getTransactionId()).thenReturn(transactionId);
    when(event.getEventType()).thenReturn(null);

    Transaction transaction = new Transaction();
    transaction.setId(transactionId);
    transaction.setOrgId(orgId);

    when(transactionRepository.findById(transactionId))
            .thenReturn(Optional.of(transaction));

    when(failedEventRecordRepository.insertIfNotExists(
            eq(eventId),
            eq(transactionId),
            eq(orgId),
            eq("UNKNOWN"),
            any(),
            any()))
            .thenReturn(1);

    doThrow(new RuntimeException("Investigation creation failed"))
            .when(investigationService)
            .createInvestigation(
                    eq(transactionId),
                    eq(orgId),
                    isNull(),
                    eq(InvestigationPriority.CRITICAL),
                    eq(InvestigationIssueType.PROCESSING_FAILURE),
                    anyString());

    RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> failedEventRecoveryService.recordFailedEvent(
                    event,
                    "Retries exhausted"));

    assertEquals(
            "Investigation creation failed",
            exception.getMessage());

    verify(failedEventRecordRepository).insertIfNotExists(
            eq(eventId),
            eq(transactionId),
            eq(orgId),
            eq("UNKNOWN"),
            eq("Retries exhausted"),
            any());
}
}