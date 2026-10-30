package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.service.ProcessedEventService;
import com.payflow.payflow_backend.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionEventConsumer.class);

    private static final String TRANSACTION_EVENTS_TOPIC =
            "transaction-events";

    private static final String CONSUMER_GROUP =
            "payflow-group";

    private final ReconciliationService reconciliationService;
    private final ProcessedEventService processedEventService;

    public TransactionEventConsumer(
            ReconciliationService reconciliationService,
            ProcessedEventService processedEventService) {

        this.reconciliationService = reconciliationService;
        this.processedEventService = processedEventService;
    }

    @Transactional
    @KafkaListener(
            topics = TRANSACTION_EVENTS_TOPIC,
            groupId = CONSUMER_GROUP
    )
    public void consume(TransactionEvent event) {

        if (event == null
                || event.getEventId() == null
                || event.getEventType() == null
                || event.getOrgId() == null) {

            throw new IllegalArgumentException(
                    "Transaction event, event ID, event type, "
                            + "and organization ID are required");
        }

        logger.info(
                "Received transaction event: eventId={}, transactionId={}, "
                        + "eventType={}, status={}, orgId={}",
                event.getEventId(),
                event.getTransactionId(),
                event.getEventType(),
                event.getStatus(),
                event.getOrgId()
        );

        // Claim the event inside the consumer's transaction.
        boolean claimed = processedEventService.tryMarkAsProcessed(
                event.getEventId(),
                event.getEventType().name(),
                event.getOrgId()
        );

        if (!claimed) {
            logger.info(
                    "Skipping already processed event: eventId={}",
                    event.getEventId()
            );
            return;
        }

        // Reconciliation must succeed before the transaction commits.
        switch (event.getEventType()) {
            case SUCCESS:
            case FAILED:
                if (event.getTransactionId() == null) {
                    throw new IllegalArgumentException(
                            "Transaction ID is required for reconciliation events");
                }

                reconciliationService.reconcileTransaction(
                        event.getTransactionId()
                );
                break;

            case CREATED:
            case PROCESSING:
                logger.debug(
                        "No reconciliation required for eventType={}",
                        event.getEventType()
                );
                break;
        }

        logger.info(
                "Successfully processed transaction event: eventId={}",
                event.getEventId()
        );
    }
}

