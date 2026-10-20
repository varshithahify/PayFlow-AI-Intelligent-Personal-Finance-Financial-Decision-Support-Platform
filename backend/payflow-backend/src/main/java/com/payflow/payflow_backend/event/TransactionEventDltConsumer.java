package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.service.FailedEventRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventDltConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    TransactionEventDltConsumer.class
            );

    private static final String DLT_TOPIC =
            "transaction-events-dlt";

    private static final String DLT_CONSUMER_GROUP =
            "payflow-dlt-group";

    private final FailedEventRecoveryService failedEventRecoveryService;

    public TransactionEventDltConsumer(
            FailedEventRecoveryService failedEventRecoveryService) {

        this.failedEventRecoveryService =
                failedEventRecoveryService;
    }

    @KafkaListener(
            topics = DLT_TOPIC,
            groupId = DLT_CONSUMER_GROUP
    )
    public void consume(TransactionEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "DLT transaction event must not be null"
            );
        }

        logger.error(
                "DLT EVENT RECEIVED: eventId={}, transactionId={}, "
                        + "eventType={}, status={}, amount={}, currency={}",
                event.getEventId(),
                event.getTransactionId(),
                event.getEventType(),
                event.getStatus(),
                event.getAmount(),
                event.getCurrency()
        );

        String failureReason =
                "Transaction event exhausted its configured retries "
                        + "and reached the dead-letter topic.";

        boolean recorded =
                failedEventRecoveryService.recordFailedEvent(
                        event,
                        failureReason
                );

        if (recorded) {
            logger.error(
                    "Failed event recorded for investigation: "
                            + "eventId={}, transactionId={}",
                    event.getEventId(),
                    event.getTransactionId()
            );
        } else {
            logger.info(
                    "Duplicate DLT event already recorded: "
                            + "eventId={}, transactionId={}",
                    event.getEventId(),
                    event.getTransactionId()
            );
        }
    }
}