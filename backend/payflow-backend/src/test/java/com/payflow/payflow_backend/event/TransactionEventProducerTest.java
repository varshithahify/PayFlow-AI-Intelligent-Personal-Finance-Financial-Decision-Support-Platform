package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.TransactionStatus;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TransactionEventProducerTest {

    private KafkaTemplate<String, Object> kafkaTemplate;

    private TransactionEventProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new TransactionEventProducer(kafkaTemplate);
    }

    @Test
    void shouldPublishEventToTransactionEventsTopic() {
        TransactionEvent event = createEvent();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("transaction-events", 0),
                0L,
                0,
                System.currentTimeMillis(),
                0,
                0
        );

        SendResult<String, Object> sendResult =
                new SendResult<>(null, metadata);

        when(kafkaTemplate.send(
                "transaction-events",
                "100",
                event
        )).thenReturn(
                CompletableFuture.completedFuture(sendResult)
        );

        CompletableFuture<Void> result = producer.publish(event);

        assertDoesNotThrow(result::join);

        verify(kafkaTemplate).send(
                "transaction-events",
                "100",
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
    void shouldRejectEventWithoutTransactionId() {
        TransactionEvent event = createEvent();
        event.setTransactionId(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithoutEventId() {
        TransactionEvent event = createEvent();
        event.setEventId(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> producer.publish(event)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    private TransactionEvent createEvent() {
        TransactionEvent event = new TransactionEvent(
                100L,
                1L,
                1L,
                new BigDecimal("250.00"),
                "INR",
                "CARD",
                TransactionStatus.SUCCESS,
                TransactionEventType.SUCCESS,
                LocalDateTime.now()
        );

        event.setEventId(UUID.randomUUID());

        return event;
    }
}