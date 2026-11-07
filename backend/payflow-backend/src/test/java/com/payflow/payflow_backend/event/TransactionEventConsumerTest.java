package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.service.ProcessedEventService;
import com.payflow.payflow_backend.service.ReconciliationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionEventConsumerTest {

    private ReconciliationService reconciliationService;

    private ProcessedEventService processedEventService;

    private TransactionEventConsumer consumer;

    @BeforeEach
    void setUp() {

        reconciliationService = mock(ReconciliationService.class);

        processedEventService = mock(ProcessedEventService.class);

        consumer = new TransactionEventConsumer(
            reconciliationService,
            processedEventService
        );
    }

    // --------------------------------------------------
    // 1. Reject null event
    // --------------------------------------------------

    @Test
    void shouldRejectNullEvent() {

        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(null)
        );

        verifyNoInteractions(
            processedEventService,
            reconciliationService
        );
    }

    // --------------------------------------------------
    // 2. Reject missing event ID
    // --------------------------------------------------

    @Test
    void shouldRejectEventWithoutEventId() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        event.setEventId(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(event)
        );

        verifyNoInteractions(
            processedEventService,
            reconciliationService
        );
    }

    // --------------------------------------------------
    // 3. Reject missing event type
    // --------------------------------------------------

    @Test
    void shouldRejectEventWithoutEventType() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        event.setEventType(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(event)
        );

        verifyNoInteractions(
            processedEventService,
            reconciliationService
        );
    }

    // --------------------------------------------------
    // 4. Reject missing organization ID
    // --------------------------------------------------

    @Test
    void shouldRejectEventWithoutOrganizationId() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        event.setOrgId(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(event)
        );

        verifyNoInteractions(
            processedEventService,
            reconciliationService
        );
    }

    // --------------------------------------------------
    // 5. Skip already processed event
    // --------------------------------------------------

    @Test
    void shouldSkipAlreadyProcessedEvent() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            event.getEventType().name(),
            event.getOrgId()
        )).thenReturn(false);

        assertDoesNotThrow(() -> consumer.consume(event));

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "SUCCESS",
            1L
        );

        verifyNoInteractions(reconciliationService);
    }

    // --------------------------------------------------
    // 6. Reconcile successful transaction event
    // --------------------------------------------------

    @Test
    void shouldReconcileSuccessfulTransactionEvent() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "SUCCESS",
            1L
        )).thenReturn(true);

        assertDoesNotThrow(() -> consumer.consume(event));

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "SUCCESS",
            1L
        );

        verify(reconciliationService).reconcileTransaction(
            100L,
            1L
        );
    }

    // --------------------------------------------------
    // 7. Reconcile failed transaction event
    // --------------------------------------------------

    @Test
    void shouldReconcileFailedTransactionEvent() {

        TransactionEvent event = createEvent(
            TransactionEventType.FAILED
        );

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "FAILED",
            1L
        )).thenReturn(true);

        assertDoesNotThrow(() -> consumer.consume(event));

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "FAILED",
            1L
        );

        verify(reconciliationService).reconcileTransaction(
            100L,
            1L
        );
    }

    // --------------------------------------------------
    // 8. CREATED event does not require reconciliation
    // --------------------------------------------------

    @Test
    void shouldNotReconcileCreatedEvent() {

        TransactionEvent event = createEvent(
            TransactionEventType.CREATED
        );

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "CREATED",
            1L
        )).thenReturn(true);

        assertDoesNotThrow(() -> consumer.consume(event));

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "CREATED",
            1L
        );

        verifyNoInteractions(reconciliationService);
    }

    // --------------------------------------------------
    // 9. PROCESSING event does not require reconciliation
    // --------------------------------------------------

    @Test
    void shouldNotReconcileProcessingEvent() {

        TransactionEvent event = createEvent(
            TransactionEventType.PROCESSING
        );

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "PROCESSING",
            1L
        )).thenReturn(true);

        assertDoesNotThrow(() -> consumer.consume(event));

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "PROCESSING",
            1L
        );

        verifyNoInteractions(reconciliationService);
    }

    // --------------------------------------------------
    // 10. Reject SUCCESS event without transaction ID
    // --------------------------------------------------

    @Test
    void shouldRejectSuccessfulEventWithoutTransactionId() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        event.setTransactionId(null);

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "SUCCESS",
            1L
        )).thenReturn(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(event)
        );

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "SUCCESS",
            1L
        );

        verifyNoInteractions(reconciliationService);
    }

    // --------------------------------------------------
    // 11. Reject FAILED event without transaction ID
    // --------------------------------------------------

    @Test
    void shouldRejectFailedEventWithoutTransactionId() {

        TransactionEvent event = createEvent(
            TransactionEventType.FAILED
        );

        event.setTransactionId(null);

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "FAILED",
            1L
        )).thenReturn(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> consumer.consume(event)
        );

        verify(processedEventService).tryMarkAsProcessed(
            event.getEventId(),
            "FAILED",
            1L
        );

        verifyNoInteractions(reconciliationService);
    }

    // --------------------------------------------------
    // 12. Verify organization ID is passed to reconciliation
    // --------------------------------------------------

    @Test
    void shouldPassOrganizationIdToReconciliationService() {

        TransactionEvent event = createEvent(
            TransactionEventType.SUCCESS
        );

        event.setOrgId(25L);

        when(processedEventService.tryMarkAsProcessed(
            event.getEventId(),
            "SUCCESS",
            25L
        )).thenReturn(true);

        consumer.consume(event);

        verify(reconciliationService).reconcileTransaction(
            100L,
            25L
        );
    }

    // --------------------------------------------------
    // Helper method
    // --------------------------------------------------

    private TransactionEvent createEvent(
        TransactionEventType eventType
    ) {

        TransactionEvent event = new TransactionEvent(
            100L,
            1L,
            1L,
            new BigDecimal("250.00"),
            "INR",
            "CARD",
            TransactionStatus.SUCCESS,
            eventType,
            LocalDateTime.now()
        );

        event.setEventId(UUID.randomUUID());

        return event;
    }
}