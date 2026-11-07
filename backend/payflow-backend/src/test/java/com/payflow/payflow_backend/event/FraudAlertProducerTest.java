package com.payflow.payflow_backend.event;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAlertProducerTest {

    private static final String TOPIC = "fraud-alerts";
    private static final Long TRANSACTION_ID = 100L;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private FraudAlertProducer fraudAlertProducer;

    @Test
void shouldPublishFraudAlertSuccessfully() {

    FraudAlertEvent event = validEvent();

    SendResult<String, Object> sendResult = mock(SendResult.class);

    RecordMetadata metadata = new RecordMetadata(
            new TopicPartition(TOPIC, 1),
            0L,
            0,
            System.currentTimeMillis(),
            0,
            0
    );

    when(kafkaTemplate.send(TOPIC, TRANSACTION_ID.toString(), event))
            .thenReturn(CompletableFuture.completedFuture(sendResult));

    when(sendResult.getRecordMetadata()).thenReturn(metadata);

    CompletableFuture<Void> result = fraudAlertProducer.publish(event);

    assertNotNull(result);
    result.join();

    verify(kafkaTemplate).send(TOPIC, TRANSACTION_ID.toString(), event);
    verify(sendResult).getRecordMetadata();

    assertEquals(TOPIC, metadata.topic());
    assertEquals(1, metadata.partition());
    assertEquals(0L, metadata.offset());
}

    @Test
    void shouldCompleteExceptionallyWhenKafkaPublicationFails() {

        FraudAlertEvent event = validEvent();

        RuntimeException kafkaFailure =
                new RuntimeException("Kafka unavailable");

        when(kafkaTemplate.send(TOPIC, TRANSACTION_ID.toString(), event))
                .thenReturn(CompletableFuture.failedFuture(kafkaFailure));

        CompletableFuture<Void> result = fraudAlertProducer.publish(event);

        CompletionException exception = assertThrows(
                CompletionException.class,
                result::join
        );

        assertEquals(kafkaFailure, exception.getCause());

        verify(kafkaTemplate).send(TOPIC, TRANSACTION_ID.toString(), event);
    }

    @Test
    void shouldRejectNullEvent() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(null)
        );

        assertEquals(
                "Fraud alert event cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithNullEventId() {

        FraudAlertEvent event = validEvent();
        event.setEventId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(event)
        );

        assertEquals(
                "Fraud alert event ID cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithNullTransactionId() {

        FraudAlertEvent event = validEvent();
        event.setTransactionId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(event)
        );

        assertEquals(
                "Fraud alert transaction ID cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithNullOrganizationId() {

        FraudAlertEvent event = validEvent();
        event.setOrgId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(event)
        );

        assertEquals(
                "Fraud alert organization ID cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithNullAction() {

        FraudAlertEvent event = validEvent();
        event.setAction(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(event)
        );

        assertEquals(
                "Fraud alert action cannot be blank",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithBlankAction() {

        FraudAlertEvent event = validEvent();
        event.setAction("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(event)
        );

        assertEquals(
                "Fraud alert action cannot be blank",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventWithNullOccurrenceTime() {

        FraudAlertEvent event = validEvent();
        event.setOccurredAt(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fraudAlertProducer.publish(event)
        );

        assertEquals(
                "Fraud alert occurrence time cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(kafkaTemplate);
    }

    private FraudAlertEvent validEvent() {

        return new FraudAlertEvent(
                UUID.randomUUID(),
                TRANSACTION_ID,
                10L,
                "BLOCK",
                0.95,
                0.90,
                0.85,
                List.of("HIGH_VELOCITY", "SUSPICIOUS_DEVICE"),
                LocalDateTime.now()
        );
    }
}