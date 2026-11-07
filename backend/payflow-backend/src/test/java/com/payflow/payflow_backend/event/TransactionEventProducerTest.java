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
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionEventProducerTest {

    private static final String TOPIC = "transaction-events";

    private KafkaTemplate<String, Object> kafkaTemplate;

    private TransactionEventProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new TransactionEventProducer(kafkaTemplate);
    }

    // --------------------------------------------------
    // 1. Successful event publishing
    // --------------------------------------------------

    @Test
    void shouldPublishEventToTransactionEventsTopic() {

        TransactionEvent event = createEvent();

        mockSuccessfulKafkaSend(event);

        CompletableFuture<Void> result = producer.publish(event);

        assertDoesNotThrow(result::join);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());

        verify(kafkaTemplate).send(
            TOPIC,
            "100",
            event
        );
    }

    // --------------------------------------------------
    // 2. Reject null event
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
    // 3. Reject missing transaction ID
    // --------------------------------------------------

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

    // --------------------------------------------------
    // 4. Reject missing event ID
    // --------------------------------------------------

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

    // --------------------------------------------------
    // 5. Handle Kafka publishing failure
    // --------------------------------------------------

    @Test
    void shouldHandleKafkaPublishingFailure() {

        TransactionEvent event = createEvent();

        CompletableFuture<SendResult<String, Object>> failedFuture =
            CompletableFuture.failedFuture(
                new RuntimeException("Kafka broker unavailable")
            );

        when(kafkaTemplate.send(
            TOPIC,
            "100",
            event
        )).thenReturn(failedFuture);

        CompletableFuture<Void> result = producer.publish(event);

        assertThrows(
            CompletionException.class,
            result::join
        );

        assertTrue(result.isCompletedExceptionally());

        verify(kafkaTemplate).send(
            TOPIC,
            "100",
            event
        );
    }

    // --------------------------------------------------
    // 6. Verify transaction ID is used as Kafka key
    // --------------------------------------------------

    @Test
    void shouldUseTransactionIdAsKafkaMessageKey() {

        TransactionEvent event = createEvent();

        mockSuccessfulKafkaSend(event);

        producer.publish(event).join();

        verify(kafkaTemplate).send(
            TOPIC,
            String.valueOf(event.getTransactionId()),
            event
        );
    }

    // --------------------------------------------------
    // 7. Verify topic name
    // --------------------------------------------------

    @Test
    void shouldPublishToCorrectKafkaTopic() {

        TransactionEvent event = createEvent();

        mockSuccessfulKafkaSend(event);

        producer.publish(event).join();

        verify(kafkaTemplate).send(
            eq("transaction-events"),
            anyString(),
            eq(event)
        );
    }

    // --------------------------------------------------
    // 8. Verify successful future completion
    // --------------------------------------------------

    @Test
    void shouldCompleteFutureSuccessfullyAfterPublishing() {

        TransactionEvent event = createEvent();

        mockSuccessfulKafkaSend(event);

        CompletableFuture<Void> result = producer.publish(event);

        assertDoesNotThrow(result::join);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
    }

    // --------------------------------------------------
    // 9. Verify failed future completion
    // --------------------------------------------------

    @Test
    void shouldCompleteFutureExceptionallyWhenKafkaFails() {

        TransactionEvent event = createEvent();

        when(kafkaTemplate.send(
            TOPIC,
            "100",
            event
        )).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Kafka connection failed")
            )
        );

        CompletableFuture<Void> result = producer.publish(event);

        assertTrue(result.isCompletedExceptionally());

        verify(kafkaTemplate).send(
            TOPIC,
            "100",
            event
        );
    }

    // --------------------------------------------------
    // 10. Verify event is passed unchanged
    // --------------------------------------------------

    @Test
    void shouldPublishOriginalTransactionEventWithoutModification() {

        TransactionEvent event = createEvent();

        mockSuccessfulKafkaSend(event);

        producer.publish(event).join();

        verify(kafkaTemplate).send(
            TOPIC,
            "100",
            event
        );

        assertEquals(100L, event.getTransactionId());
        assertEquals(1L, event.getUserId());
        assertEquals(1L, event.getOrgId());
        assertEquals(new BigDecimal("250.00"), event.getAmount());
        assertEquals("INR", event.getCurrency());
        assertEquals("CARD", event.getPaymentMethod());
        assertEquals(TransactionStatus.SUCCESS, event.getStatus());
        assertEquals(TransactionEventType.SUCCESS, event.getEventType());
    }

    // --------------------------------------------------
    // Helper: Create transaction event
    // --------------------------------------------------

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

    // --------------------------------------------------
    // Helper: Mock successful Kafka response
    // --------------------------------------------------

    private void mockSuccessfulKafkaSend(TransactionEvent event) {

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
            String.valueOf(event.getTransactionId()),
            event
        )).thenReturn(
            CompletableFuture.completedFuture(sendResult)
        );
    }
}