package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventDltConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    TransactionEventDltConsumer.class);

    private static final String DLT_TOPIC =
            "transaction-events-dlt";

    private static final String DLT_CONSUMER_GROUP =
            "payflow-dlt-group";

    private final ReconciliationService reconciliationService;

    public TransactionEventDltConsumer(
            ReconciliationService reconciliationService) {

        this.reconciliationService =
                reconciliationService;
    }

    @KafkaListener(
            topics = DLT_TOPIC,
            groupId = DLT_CONSUMER_GROUP
    )
    public void consume(TransactionEvent event) {

        logger.error(
                "DLT EVENT RECEIVED: eventId={}, transactionId={}, eventType={}, status={}, amount={}, currency={}",
                event.getEventId(),
                event.getTransactionId(),
                event.getEventType(),
                event.getStatus(),
                event.getAmount(),
                event.getCurrency()
        );

        logger.error(
                "Failed transaction event requires investigation: transactionId={}",
                event.getTransactionId()
        );

        reconciliationService.reconcileTransaction(
                event.getTransactionId());

        logger.info(
                "DLT event reconciliation completed: transactionId={}",
                event.getTransactionId()
        );
    }
}