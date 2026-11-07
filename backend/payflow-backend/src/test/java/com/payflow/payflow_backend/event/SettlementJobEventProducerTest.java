package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.event.SettlementJobEvent;
import com.payflow.payflow_backend.event.SettlementJobEventType;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SettlementJobEventProducerTest {

    private static final String TOPIC = "settlement-jobs";

    private KafkaTemplate<String, Object> kafkaTemplate;

    private SettlementJobEventProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new SettlementJobEventProducer(kafkaTemplate);
    }

    // --------------------------------------------------
    // 1. Successful event publishing
    // --------------------------------------------------

    @Test
    void shouldPublishEventToSettlementJobsTopicUsingJobIdAsKey() {

        SettlementJobEvent event = createEvent();

        mockSuccessfulKafkaSend(event);

        assertDoesNotThrow(() -> producer.publish(event));

        verify(kafkaTemplate).send(
            TOPIC,
            event.getJobId(),
            event
        );
    }

    // --------------------------------------------------
    // 2. Kafka publishing failure
    // --------------------------------------------------

    @Test
    void shouldHandleKafkaPublishingFailure() {

        SettlementJobEvent event = createEvent();

        CompletableFuture<SendResult<String, Object>> failedFuture =
            CompletableFuture.failedFuture(
                new RuntimeException("Kafka broker unavailable")
            );

        when(kafkaTemplate.send(
            TOPIC,
            event.getJobId(),
            event
        )).thenReturn(failedFuture);

        assertDoesNotThrow(() -> producer.publish(event));

        verify(kafkaTemplate).send(
            TOPIC,
            event.getJobId(),
            event
        );
    }

    // --------------------------------------------------
    // 3. Null event validation
    // --------------------------------------------------

    @Test
    void shouldRejectNullEvent() {

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(null)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 4. Missing event ID
    // --------------------------------------------------

    @Test
    void shouldRejectEventWithoutEventId() {

        SettlementJobEvent event = createEvent();
        event.setEventId(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 5. Blank job ID
    // --------------------------------------------------

    @Test
    void shouldRejectBlankJobId() {

        SettlementJobEvent event = createEvent();
        event.setJobId(" ");

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 6. Missing organization ID
    // --------------------------------------------------

    @Test
    void shouldRejectMissingOrganizationId() {

        SettlementJobEvent event = createEvent();
        event.setOrgId(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 7. Negative transaction count
    // --------------------------------------------------

    @Test
    void shouldRejectNegativeTransactionCount() {

        SettlementJobEvent event = createEvent();
        event.setTransactionCount(-1);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 8. Missing job ID
    // --------------------------------------------------

    @Test
    void shouldRejectNullJobId() {

        SettlementJobEvent event = createEvent();
        event.setJobId(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 9. Missing event type
    // --------------------------------------------------

    @Test
    void shouldRejectNullEventType() {

        SettlementJobEvent event = createEvent();
        event.setEventType(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 10. Missing occurrence timestamp
    // --------------------------------------------------

    @Test
    void shouldRejectNullOccurredAt() {

        SettlementJobEvent event = createEvent();
        event.setOccurredAt(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 11. Negative discrepancy count
    // --------------------------------------------------

    @Test
    void shouldRejectNegativeDiscrepancyCount() {

        SettlementJobEvent event = createEvent();
        event.setDiscrepancyCount(-1);

        assertThrows(
            IllegalArgumentException.class,
            () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    // --------------------------------------------------
    // 12. Null transaction count is allowed
    // --------------------------------------------------

    @Test
    void shouldAllowNullTransactionCount() {

        SettlementJobEvent event = createEvent();
        event.setTransactionCount(null);

        mockSuccessfulKafkaSend(event);

        assertDoesNotThrow(() -> producer.publish(event));

        verify(kafkaTemplate).send(
            TOPIC,
            event.getJobId(),
            event
        );
    }

    // --------------------------------------------------
    // 13. Null discrepancy count is allowed
    // --------------------------------------------------

    @Test
    void shouldAllowNullDiscrepancyCount() {

        SettlementJobEvent event = createEvent();
        event.setDiscrepancyCount(null);

        mockSuccessfulKafkaSend(event);

        assertDoesNotThrow(() -> producer.publish(event));

        verify(kafkaTemplate).send(
            TOPIC,
            event.getJobId(),
            event
        );
    }

    // --------------------------------------------------
    // 14. Zero counts are allowed
    // --------------------------------------------------

    @Test
    void shouldAllowZeroTransactionAndDiscrepancyCounts() {

        SettlementJobEvent event = createEvent();

        event.setTransactionCount(0);
        event.setDiscrepancyCount(0);

        mockSuccessfulKafkaSend(event);

        assertDoesNotThrow(() -> producer.publish(event));

        verify(kafkaTemplate).send(
            TOPIC,
            event.getJobId(),
            event
        );
    }

    // --------------------------------------------------
    // Helper methods
    // --------------------------------------------------

    private SettlementJobEvent createEvent() {

        return new SettlementJobEvent(
            UUID.randomUUID(),
            "settlement-2026-001",
            1L,
            SettlementJobEventType.STARTED,
            0,
            0,
            "Settlement job started",
            LocalDateTime.now()
        );
    }

    private void mockSuccessfulKafkaSend(SettlementJobEvent event) {

        RecordMetadata metadata = new RecordMetadata(
            new TopicPartition(TOPIC, 0),
            0L,
            0,
            System.currentTimeMillis(),
            0,
            0
        );

        SendResult<String, Object> sendResult =
            new SendResult<>(null, metadata);

        when(kafkaTemplate.send(
            TOPIC,
            event.getJobId(),
            event
        )).thenReturn(
            CompletableFuture.completedFuture(sendResult)
        );
    }
}