package com.payflow.payflow_backend.event;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class FraudAlertProducer {

    private static final Logger log =
            LoggerFactory.getLogger(FraudAlertProducer.class);

    private static final String FRAUD_ALERTS_TOPIC = "fraud-alerts";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FraudAlertProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<Void> publish(FraudAlertEvent event) {

        validate(event);

        String key = event.getTransactionId().toString();

        CompletableFuture<SendResult<String, Object>> sendFuture =
                kafkaTemplate.send(
                        FRAUD_ALERTS_TOPIC,
                        key,
                        event);

        return sendFuture.thenAccept(result -> {

            RecordMetadata metadata = result.getRecordMetadata();

            log.info(
                    "Fraud alert published: transactionId={}, action={}, topic={}, partition={}, offset={}",
                    event.getTransactionId(),
                    event.getAction(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());

        }).whenComplete((ignored, error) -> {

            if (error != null) {
                log.error(
                        "Failed to publish fraud alert: transactionId={}, action={}",
                        event.getTransactionId(),
                        event.getAction(),
                        error);
            }
        });
    }

    private void validate(FraudAlertEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Fraud alert event cannot be null");
        }

        if (event.getEventId() == null) {
            throw new IllegalArgumentException(
                    "Fraud alert event ID cannot be null");
        }

        if (event.getTransactionId() == null) {
            throw new IllegalArgumentException(
                    "Fraud alert transaction ID cannot be null");
        }

        if (event.getOrgId() == null) {
            throw new IllegalArgumentException(
                    "Fraud alert organization ID cannot be null");
        }

        if (event.getAction() == null
                || event.getAction().isBlank()) {
            throw new IllegalArgumentException(
                    "Fraud alert action cannot be blank");
        }

        if (event.getOccurredAt() == null) {
            throw new IllegalArgumentException(
                    "Fraud alert occurrence time cannot be null");
        }
    }
}