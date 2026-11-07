package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.repository.ProcessedEventRepository;
import com.payflow.payflow_backend.service.ReconciliationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class TransactionEventConsumerTransactionTest {

    @Autowired
    private TransactionEventConsumer transactionEventConsumer;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @Test
    void shouldRollbackProcessedEventWhenReconciliationFails() {

        TransactionEvent event = createSuccessEvent(UUID.randomUUID());

        doThrow(new RuntimeException("Simulated reconciliation failure"))
                .when(reconciliationService)
                .reconcileTransaction(12L, 1L);

        assertThrows(
                RuntimeException.class,
                () -> transactionEventConsumer.consume(event)
        );

        assertFalse(
                processedEventRepository.existsByEventId(event.getEventId())
        );
    }

    @Test
    void shouldAllowRetryAfterFailureAndIgnoreDuplicateAfterSuccess() {

        UUID eventId = UUID.randomUUID();
        TransactionEvent event = createSuccessEvent(eventId);

        doThrow(new RuntimeException("Temporary reconciliation failure"))
                .doReturn(null)
                .when(reconciliationService)
                .reconcileTransaction(12L, 1L);

        // First delivery should fail.
        assertThrows(
                RuntimeException.class,
                () -> transactionEventConsumer.consume(event)
        );

        // Failed processing should roll back the event record.
        assertFalse(
                processedEventRepository.existsByEventId(eventId)
        );

        // Second delivery should succeed.
        transactionEventConsumer.consume(event);

        assertTrue(
                processedEventRepository.existsByEventId(eventId)
        );

        // Third delivery should be ignored as a duplicate.
        transactionEventConsumer.consume(event);

        verify(reconciliationService, times(2))
                .reconcileTransaction(12L, 1L);
    }

    @Test
    void shouldProcessConcurrentDuplicateEventOnlyOnce() throws Exception {

        UUID eventId = UUID.randomUUID();
        TransactionEvent event = createSuccessEvent(eventId);

        CountDownLatch reconciliationStarted = new CountDownLatch(1);
        CountDownLatch allowReconciliationToFinish = new CountDownLatch(1);

        doAnswer(invocation -> {

            reconciliationStarted.countDown();

            if (!allowReconciliationToFinish.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting to finish reconciliation"
                );
            }

            return null;

        }).when(reconciliationService)
                .reconcileTransaction(12L, 1L);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<?> firstDelivery = executor.submit(
                    () -> transactionEventConsumer.consume(event)
            );

            assertTrue(
                    reconciliationStarted.await(10, TimeUnit.SECONDS),
                    "First delivery should reach reconciliation"
            );

            Future<?> secondDelivery = executor.submit(
                    () -> transactionEventConsumer.consume(event)
            );

            // Allow the first transaction to finish.
            allowReconciliationToFinish.countDown();

            firstDelivery.get(15, TimeUnit.SECONDS);
            secondDelivery.get(15, TimeUnit.SECONDS);

            assertTrue(
                    processedEventRepository.existsByEventId(eventId),
                    "The event should be recorded as processed"
            );

            verify(reconciliationService, times(1))
                    .reconcileTransaction(12L, 1L);

        } finally {

            allowReconciliationToFinish.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Executor should shut down"
            );
        }
    }

    private TransactionEvent createSuccessEvent(UUID eventId) {

        TransactionEvent event = new TransactionEvent(
                12L,
                3L,
                1L,
                new BigDecimal("5000.00"),
                "INR",
                "UPI",
                com.payflow.payflow_backend.entity.TransactionStatus.SUCCESS,
                TransactionEventType.SUCCESS,
                LocalDateTime.now()
        );

        event.setEventId(eventId);

        return event;
    }
}