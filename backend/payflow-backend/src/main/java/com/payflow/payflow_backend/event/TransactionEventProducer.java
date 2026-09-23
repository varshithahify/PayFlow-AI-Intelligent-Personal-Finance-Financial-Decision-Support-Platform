package com.payflow.payflow_backend.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TransactionEventProducer {

    private static final String TOPIC =
            "transaction-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<?> publish(
            TransactionEvent event) {

        return kafkaTemplate.send(
                TOPIC,
                event.getTransactionId().toString(),
                event);
    }
}