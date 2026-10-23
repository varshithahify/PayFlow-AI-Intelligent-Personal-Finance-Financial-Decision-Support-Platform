package com.payflow.payflow_backend.event;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes settlement job lifecycle events to Kafka.
 */
@Service
public class SettlementJobEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(SettlementJobEventProducer.class);

    private static final String SETTLEMENT_JOBS_TOPIC = "settlement-jobs";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SettlementJobEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a settlement job event.
     *
     * The settlement job ID is used as the Kafka message key so events
     * for the same job are routed consistently to the same partition.
     *
     * @param event settlement event to publish
     * @return future that completes when the send succeeds or fails
     */
    public CompletableFuture<Void> publish(SettlementJobEvent event) {

        validate(event);

        CompletableFuture<SendResult<String, Object>> sendFuture =
                kafkaTemplate.send(
                        SETTLEMENT_JOBS_TOPIC,
                        event.getJobId(),
                        event
                );

        return sendFuture
                .thenAccept(result -> {

                    RecordMetadata metadata =
                            result.getRecordMetadata();

                    log.info(
                            "Settlement job event published: " +
                                    "eventId={}, jobId={}, eventType={}, " +
                                    "topic={}, partition={}, offset={}",
                            event.getEventId(),
                            event.getJobId(),
                            event.getEventType(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset()
                    );
                })
                .whenComplete((ignored, error) -> {

                    if (error != null) {

                        log.error(
                                "Failed to publish settlement job event: " +
                                        "eventId={}, jobId={}, eventType={}",
                                event.getEventId(),
                                event.getJobId(),
                                event.getEventType(),
                                error
                        );
                    }
                });
    }

    /**
     * Validates the event before publishing it.
     */
    private void validate(SettlementJobEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Settlement job event cannot be null"
            );
        }

        if (event.getEventId() == null) {
            throw new IllegalArgumentException(
                    "Settlement job event ID cannot be null"
            );
        }

        if (event.getJobId() == null
                || event.getJobId().isBlank()) {

            throw new IllegalArgumentException(
                    "Settlement job ID cannot be blank"
            );
        }

        if (event.getOrgId() == null) {
            throw new IllegalArgumentException(
                    "Settlement job organization ID cannot be null"
            );
        }

        if (event.getEventType() == null) {
            throw new IllegalArgumentException(
                    "Settlement job event type cannot be null"
            );
        }

        if (event.getOccurredAt() == null) {
            throw new IllegalArgumentException(
                    "Settlement job event occurrence time cannot be null"
            );
        }

        if (event.getTransactionCount() != null
                && event.getTransactionCount() < 0) {

            throw new IllegalArgumentException(
                    "Transaction count cannot be negative"
            );
        }

        if (event.getDiscrepancyCount() != null
                && event.getDiscrepancyCount() < 0) {

            throw new IllegalArgumentException(
                    "Discrepancy count cannot be negative"
            );
        }
    }
}