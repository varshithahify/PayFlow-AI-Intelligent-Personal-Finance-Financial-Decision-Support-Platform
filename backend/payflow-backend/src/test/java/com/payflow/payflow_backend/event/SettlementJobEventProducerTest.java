package com.payflow.payflow_backend.event;

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

    private KafkaTemplate<String, Object> kafkaTemplate;

    private SettlementJobEventProducer producer;

    @BeforeEach
    void setUp() {

        kafkaTemplate = mock(KafkaTemplate.class);

        producer = new SettlementJobEventProducer(kafkaTemplate);
    }

    @Test
    void shouldPublishEventToSettlementJobsTopicUsingJobIdAsKey() {

        SettlementJobEvent event = createEvent();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("settlement-jobs", 0),
                0L,
                0,
                System.currentTimeMillis(),
                0,
                0
        );

        SendResult<String, Object> sendResult =
                new SendResult<>(null, metadata);

        when(
                kafkaTemplate.send(
                        "settlement-jobs",
                        "settlement-2026-001",
                        event
                )
        ).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        assertDoesNotThrow(
                () -> producer.publish(event).join()
        );

        verify(kafkaTemplate).send(
                "settlement-jobs",
                "settlement-2026-001",
                event
        );
    }

    @Test
    void shouldRejectNullEvent() {

        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publish(null)
        );

        verifyNoInteractions(kafkaTemplate);
    }

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

    @Test
    void shouldRejectBlankJobId() {

        SettlementJobEvent event = createEvent();

        event.setJobId("  ");

        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

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
}