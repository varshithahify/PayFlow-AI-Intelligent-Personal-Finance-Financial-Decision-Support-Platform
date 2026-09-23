package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    TransactionEventConsumer.class);

    private static final String TRANSACTION_EVENTS_TOPIC =
            "transaction-events";

    private static final String CONSUMER_GROUP =
            "payflow-group";

    private final ReconciliationService reconciliationService;

    public TransactionEventConsumer(
            ReconciliationService reconciliationService) {

        this.reconciliationService =
                reconciliationService;
    }

    @KafkaListener(
            topics = TRANSACTION_EVENTS_TOPIC,
            groupId = CONSUMER_GROUP
    )
    public void consume(TransactionEvent event) {

        logger.info(
                "Received transaction event: eventId={}, transactionId={}, eventType={}, status={}",
                event.getEventId(),
                event.getTransactionId(),
                event.getEventType(),
                event.getStatus()
        );

        switch (event.getEventType()) {

            case SUCCESS:
            case FAILED:

                logger.info(
                        "Triggering reconciliation for transactionId={}",
                        event.getTransactionId()
                );

                reconciliationService.reconcileTransaction(
                        event.getTransactionId());

                break;

            case CREATED:
            case PROCESSING:

                logger.debug(
                        "No reconciliation required for eventType={}",
                        event.getEventType()
                );

                break;
        }
    }
}