package com.payflow.payflow_backend.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventProducer {

    private static final String TRANSACTION_EVENTS_TOPIC =
            "transaction-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {

        String key =
                String.valueOf(event.getTransactionId());

        kafkaTemplate.send(
                TRANSACTION_EVENTS_TOPIC,
                key,
                event);
    }
}