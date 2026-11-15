package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.service.ReconciliationService;
import com.payflow.payflow_backend.service.WebhookConfigService;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:19094",
        "app.kafka.transaction-consumer-group=payflow-retry-dlt-test-group",
        "app.kafka.transaction-dlt-consumer-group=payflow-retry-dlt-test-dlt-group"
})
class TransactionEventRetryDltIntegrationTest {

    @Autowired
    private TransactionEventProducer producer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @MockitoBean
    private ReconciliationService reconciliationService;

    @MockitoBean
    private WebhookConfigService webhookConfigService;

    private UUID eventId;

    @AfterEach
    void cleanUp() {

        if (eventId != null) {

            jdbcTemplate.update(
                    "DELETE FROM failed_event_records WHERE event_id = ?",
                    eventId
            );

            jdbcTemplate.update(
                    "DELETE FROM processed_events WHERE event_id = ?",
                    eventId
            );
        }
    }

    private void waitForTransactionConsumerAssignment()
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {

            for (MessageListenerContainer container :
                    kafkaListenerEndpointRegistry.getListenerContainers()) {

                Collection<TopicPartition> partitions =
                        container.getAssignedPartitions();

                if (partitions != null &&
                        partitions.stream().anyMatch(
                                partition ->
                                        "transaction-events".equals(
                                                partition.topic()))) {
                    return;
                }
            }

            Thread.sleep(100);
        }

        fail("Transaction event consumer did not receive partition assignment");
    }

    @Test
    void shouldRetryFailedEventPublishToDltAndPersistFailure()
            throws Exception {

        Long transactionId = 987654321L;
        Long orgId = 1L;

        eventId = UUID.randomUUID();

        when(webhookConfigService.getActiveWebhooks(anyLong()))
                .thenReturn(List.of());

        doThrow(new IllegalStateException(
                "Simulated reconciliation failure"
        )).when(reconciliationService)
                .reconcileTransaction(transactionId, orgId);

        // Wait until the transaction consumer receives its partition.
        waitForTransactionConsumerAssignment();

        TransactionEvent event = new TransactionEvent(
                transactionId,
                1001L,
                orgId,
                new BigDecimal("2500.00"),
                "INR",
                "UPI",
                TransactionStatus.CREATED,
                TransactionEventType.SUCCESS,
                LocalDateTime.now()
        );

        event.setEventId(eventId);

        // Publish using the actual application producer.
        producer.publish(event).get(10, TimeUnit.SECONDS);

        // Initial attempt plus two retries.
        verify(reconciliationService, timeout(30000).times(3))
                .reconcileTransaction(transactionId, orgId);

        // Wait for the DLT consumer to persist the failed event.
        long deadline = System.currentTimeMillis() + 30000;

        long failedEventCount = 0;

        while (System.currentTimeMillis() < deadline) {

            failedEventCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM failed_event_records WHERE event_id = ?",
                    Long.class,
                    eventId
            );

            if (failedEventCount == 1) {
                break;
            }

            Thread.sleep(250);
        }

        assertEquals(
                1L,
                failedEventCount,
                "Failed event should be persisted exactly once"
        );

        Long processedEventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?",
                Long.class,
                eventId
        );

        assertEquals(
                0L,
                processedEventCount,
                "Processed-event claim should roll back after reconciliation failure"
        );

        String recordedEventType = jdbcTemplate.queryForObject(
                "SELECT event_type FROM failed_event_records WHERE event_id = ?",
                String.class,
                eventId
        );

        assertEquals("SUCCESS", recordedEventType);

        verify(reconciliationService, times(3))
                .reconcileTransaction(transactionId, orgId);
    }
}
