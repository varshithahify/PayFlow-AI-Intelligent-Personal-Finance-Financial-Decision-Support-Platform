package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.event.SettlementJobEvent;
import com.payflow.payflow_backend.event.SettlementJobEventProducer;
import com.payflow.payflow_backend.event.SettlementJobEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SettlementJobServiceTest {

    private ReconciliationService reconciliationService;
    private SettlementJobEventProducer eventProducer;
    private SettlementJobService settlementJobService;

    @BeforeEach
    void setUp() {
        reconciliationService = mock(ReconciliationService.class);
        eventProducer = mock(SettlementJobEventProducer.class);

        settlementJobService = new SettlementJobService(
                reconciliationService,
                eventProducer
        );

        when(eventProducer.publish(any(SettlementJobEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldPublishStartedAndCompletedForMatchedTransaction() {
        ReconciliationRecord record =
                createRecord(ReconciliationStatus.MATCHED);

        when(reconciliationService.reconcileTransaction(1L, 10L))
                .thenReturn(record);

        SettlementJobService.SettlementJobResult result =
                settlementJobService.executeSettlementJob(
                        "settlement-test-1",
                        10L,
                        List.of(1L)
                );

        assertEquals("settlement-test-1", result.getJobId());
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, result.getDiscrepancyCount());

        verify(reconciliationService).reconcileTransaction(1L, 10L);

        verify(eventProducer, times(2))
                .publish(any(SettlementJobEvent.class));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.STARTED
        ));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.COMPLETED
        ));
    }

    @Test
    void shouldPublishDiscrepancyWhenTransactionDoesNotMatch() {
        when(reconciliationService.reconcileTransaction(1L, 10L))
                .thenReturn(createRecord(ReconciliationStatus.MISMATCH));

        SettlementJobService.SettlementJobResult result =
                settlementJobService.executeSettlementJob(
                        "settlement-test-2",
                        10L,
                        List.of(1L)
                );

        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getDiscrepancyCount());

        verify(reconciliationService).reconcileTransaction(1L, 10L);

        verify(eventProducer).publish(argThat(event ->
                event.getEventType()
                        == SettlementJobEventType.DISCREPANCY_DETECTED
        ));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.COMPLETED
        ));
    }

    @Test
    void shouldPublishFailureWhenReconciliationThrows() {
        when(reconciliationService.reconcileTransaction(1L, 10L))
                .thenThrow(new IllegalStateException("Reconciliation failed"));

        assertThrows(
                IllegalStateException.class,
                () -> settlementJobService.executeSettlementJob(
                        "settlement-test-3",
                        10L,
                        List.of(1L)
                )
        );

        verify(reconciliationService).reconcileTransaction(1L, 10L);

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.STARTED
        ));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.FAILED
        ));

        verify(eventProducer, never()).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.COMPLETED
        ));
    }

    @Test
    void shouldRejectEmptyTransactionList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> settlementJobService.executeSettlementJob(
                        "settlement-test-4",
                        10L,
                        List.of()
                )
        );

        verifyNoInteractions(reconciliationService);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void shouldRejectNullOrganizationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> settlementJobService.executeSettlementJob(
                        "settlement-test-5",
                        null,
                        List.of(1L)
                )
        );

        verifyNoInteractions(reconciliationService);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void shouldProcessMultipleTransactionsAndCountDiscrepancies() {
        when(reconciliationService.reconcileTransaction(1L, 10L))
                .thenReturn(createRecord(ReconciliationStatus.MATCHED));

        when(reconciliationService.reconcileTransaction(2L, 10L))
                .thenReturn(createRecord(ReconciliationStatus.MISMATCH));

        when(reconciliationService.reconcileTransaction(3L, 10L))
                .thenReturn(createRecord(ReconciliationStatus.MISSING_EXTERNAL));

        SettlementJobService.SettlementJobResult result =
                settlementJobService.executeSettlementJob(
                        "settlement-multiple-1",
                        10L,
                        List.of(1L, 2L, 3L)
                );

        assertEquals("settlement-multiple-1", result.getJobId());
        assertEquals(3, result.getProcessedCount());
        assertEquals(2, result.getDiscrepancyCount());

        verify(reconciliationService).reconcileTransaction(1L, 10L);
        verify(reconciliationService).reconcileTransaction(2L, 10L);
        verify(reconciliationService).reconcileTransaction(3L, 10L);

        verify(eventProducer, times(2)).publish(argThat(event ->
                event.getEventType()
                        == SettlementJobEventType.DISCREPANCY_DETECTED
        ));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.STARTED
        ));

        verify(eventProducer).publish(argThat(event ->
                event.getEventType() == SettlementJobEventType.COMPLETED
        ));
    }

    @Test
    void shouldRejectTransactionListContainingNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> settlementJobService.executeSettlementJob(
                        "settlement-null-transaction",
                        10L,
                        Arrays.asList(1L, null, 3L)
                )
        );

        verifyNoInteractions(reconciliationService);
        verifyNoInteractions(eventProducer);
    }

    private ReconciliationRecord createRecord(ReconciliationStatus status) {
        ReconciliationRecord record = new ReconciliationRecord();

        record.setTransactionId(1L);
        record.setOrgId(10L);
        record.setStatus(status);

        return record;
    }
}

