package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.service.FailedEventRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventDltConsumerTest {

    @Mock
    private FailedEventRecoveryService failedEventRecoveryService;

    private TransactionEventDltConsumer dltConsumer;

    @BeforeEach
    void setUp() {
        dltConsumer = new TransactionEventDltConsumer(
                failedEventRecoveryService);
    }

    @Test
    void shouldRecordFailedEvent() {
        TransactionEvent event = mock(TransactionEvent.class);

        when(event.getEventId()).thenReturn(
                java.util.UUID.randomUUID());

        when(event.getTransactionId()).thenReturn(7L);

        when(failedEventRecoveryService.recordFailedEvent(
                eq(event),
                anyString()))
                .thenReturn(true);

        assertDoesNotThrow(() -> dltConsumer.consume(event));

        verify(failedEventRecoveryService).recordFailedEvent(
                eq(event),
                contains("exhausted its configured retries"));
    }

    @Test
    void shouldHandleDuplicateFailedEvent() {
        TransactionEvent event = mock(TransactionEvent.class);

        when(event.getEventId()).thenReturn(
                java.util.UUID.randomUUID());

        when(event.getTransactionId()).thenReturn(7L);

        when(failedEventRecoveryService.recordFailedEvent(
                eq(event),
                anyString()))
                .thenReturn(false);

        assertDoesNotThrow(() -> dltConsumer.consume(event));

        verify(failedEventRecoveryService, times(1))
                .recordFailedEvent(eq(event), anyString());
    }

    @Test
    void shouldPropagateRecoveryFailure() {
        TransactionEvent event = mock(TransactionEvent.class);

        when(event.getEventId()).thenReturn(
                java.util.UUID.randomUUID());

        when(event.getTransactionId()).thenReturn(7L);

        when(failedEventRecoveryService.recordFailedEvent(
                eq(event),
                anyString()))
                .thenThrow(new RuntimeException(
                        "Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> dltConsumer.consume(event));

        org.junit.jupiter.api.Assertions.assertEquals(
                "Database unavailable",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullEvent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> dltConsumer.consume(null));

        verifyNoInteractions(failedEventRecoveryService);
    }
}