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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

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

        UUID eventId = UUID.randomUUID();

        TransactionEvent event =
                new TransactionEvent(
                        12L,
                        3L,
                        new BigDecimal("5000.00"),
                        "INR",
                        "UPI",
                        com.payflow.payflow_backend.entity.TransactionStatus.SUCCESS,
                        TransactionEventType.SUCCESS,
                        LocalDateTime.now());

        event.setEventId(eventId);

        doThrow(
                new RuntimeException(
                        "Simulated reconciliation failure"))
                .when(reconciliationService)
                .reconcileTransaction(12L);

        assertThrows(
                RuntimeException.class,
                () -> transactionEventConsumer.consume(event));

        assertFalse(
                processedEventRepository.existsByEventId(eventId));
    }
}