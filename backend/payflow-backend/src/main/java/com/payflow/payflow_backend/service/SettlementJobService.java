package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.event.SettlementJobEvent;
import com.payflow.payflow_backend.event.SettlementJobEventProducer;
import com.payflow.payflow_backend.event.SettlementJobEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementJobService {

    private static final Logger log =
            LoggerFactory.getLogger(SettlementJobService.class);

    private final ReconciliationService reconciliationService;
    private final SettlementJobEventProducer eventProducer;

    public SettlementJobService(
            ReconciliationService reconciliationService,
            SettlementJobEventProducer eventProducer) {

        this.reconciliationService = reconciliationService;
        this.eventProducer = eventProducer;
    }

    /**
     * Reconciles a batch of internal transaction IDs and publishes
     * settlement lifecycle events.
     *
     * The job ID identifies this execution. It is not a payment ID.
     *
     * @param jobId unique settlement job identifier
     * @param orgId organization that owns the transactions
     * @param transactionIds transaction IDs to reconcile
     * @return settlement job summary
     */
    public SettlementJobResult executeSettlementJob(
            String jobId,
            Long orgId,
            List<Long> transactionIds) {

        validateInput(jobId, orgId, transactionIds);

        publishEvent(
                jobId,
                orgId,
                SettlementJobEventType.STARTED,
                0,
                0,
                "Settlement job started"
        );

        int processedCount = 0;
        int discrepancyCount = 0;

        try {

            for (Long transactionId : transactionIds) {

                ReconciliationRecord record =
                        reconciliationService.reconcileTransaction(
                                transactionId
                        );

                processedCount++;

                if (record.getStatus() != ReconciliationStatus.MATCHED) {

                    discrepancyCount++;

                    publishEvent(
                            jobId,
                            orgId,
                            SettlementJobEventType.DISCREPANCY_DETECTED,
                            processedCount,
                            discrepancyCount,
                            "Reconciliation discrepancy detected for transaction "
                                    + transactionId
                                    + ": "
                                    + record.getStatus()
                    );
                }
            }

            publishEvent(
                    jobId,
                    orgId,
                    SettlementJobEventType.COMPLETED,
                    processedCount,
                    discrepancyCount,
                    "Settlement job completed"
            );

            log.info(
                    "Settlement job completed: jobId={}, orgId={}, " +
                            "processedCount={}, discrepancyCount={}",
                    jobId,
                    orgId,
                    processedCount,
                    discrepancyCount
            );

            return new SettlementJobResult(
                    jobId,
                    processedCount,
                    discrepancyCount
            );

        } catch (RuntimeException exception) {

            try {

                publishEvent(
                        jobId,
                        orgId,
                        SettlementJobEventType.FAILED,
                        processedCount,
                        discrepancyCount,
                        "Settlement job failed: "
                                + exception.getClass().getSimpleName()
                );

            } catch (RuntimeException eventException) {

                exception.addSuppressed(eventException);

                log.error(
                        "Could not publish settlement failure event: jobId={}",
                        jobId,
                        eventException
                );
            }

            log.error(
                    "Settlement job failed: jobId={}, processedCount={}, " +
                            "discrepancyCount={}",
                    jobId,
                    processedCount,
                    discrepancyCount,
                    exception
            );

            throw exception;
        }
    }

    private void publishEvent(
            String jobId,
            Long orgId,
            SettlementJobEventType eventType,
            int transactionCount,
            int discrepancyCount,
            String details) {

        SettlementJobEvent event = new SettlementJobEvent(
                UUID.randomUUID(),
                jobId,
                orgId,
                eventType,
                transactionCount,
                discrepancyCount,
                details,
                LocalDateTime.now()
        );

        /*
         * Wait for Kafka's send result so that a publishing failure
         * is visible to the settlement workflow.
         */
        eventProducer.publish(event).join();
    }

    private void validateInput(
            String jobId,
            Long orgId,
            List<Long> transactionIds) {

        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException(
                    "Settlement job ID cannot be blank"
            );
        }

        if (orgId == null) {
            throw new IllegalArgumentException(
                    "Organization ID cannot be null"
            );
        }

        if (transactionIds == null || transactionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Settlement job must contain at least one transaction"
            );
        }

        if (transactionIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException(
                    "Settlement transaction IDs cannot contain null values"
            );
        }
    }

    /**
     * Summary returned after successful job execution.
     */
    public static class SettlementJobResult {

        private final String jobId;
        private final int processedCount;
        private final int discrepancyCount;

        public SettlementJobResult(
                String jobId,
                int processedCount,
                int discrepancyCount) {

            this.jobId = jobId;
            this.processedCount = processedCount;
            this.discrepancyCount = discrepancyCount;
        }

        public String getJobId() {
            return jobId;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public int getDiscrepancyCount() {
            return discrepancyCount;
        }
    }
}