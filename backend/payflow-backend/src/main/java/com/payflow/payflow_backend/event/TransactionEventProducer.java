package com.payflow.payflow_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TransactionEventProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionEventProducer.class);

    private static final String TRANSACTION_EVENTS_TOPIC =
            "transaction-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<Void> publish(TransactionEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Transaction event must not be null");
        }

        if (event.getTransactionId() == null) {
            throw new IllegalArgumentException(
                    "Transaction event must contain a transaction ID");
        }

        if (event.getEventId() == null) {
            throw new IllegalArgumentException(
                    "Transaction event must contain an event ID");
        }

        String key = String.valueOf(event.getTransactionId());

        return kafkaTemplate.send(
                        TRANSACTION_EVENTS_TOPIC,
                        key,
                        event)
                .thenApply(result -> {
                    logger.info(
                            "Published transaction event: eventId={}, transactionId={}, topic={}, partition={}",
                            event.getEventId(),
                            event.getTransactionId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition());

                    return (Void) null;
                })
                .whenComplete((ignored, exception) -> {
                    if (exception != null) {
                        logger.error(
                                "Failed to publish transaction event: eventId={}, transactionId={}",
                                event.getEventId(),
                                event.getTransactionId(),
                                exception);
                    }
                });
    }
}