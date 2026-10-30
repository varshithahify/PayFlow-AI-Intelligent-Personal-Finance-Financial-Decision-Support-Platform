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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * Reconciles transactions belonging to the specified organization.
     *
     * ReconciliationService must verify that each transaction
     * belongs to the supplied organization before processing it.
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
                                transactionId,
                                orgId
                        );

                if (record == null) {
                    throw new IllegalStateException(
                            "Reconciliation returned no record for transaction "
                                    + transactionId
                    );
                }

                processedCount++;

                if (record.getStatus() != ReconciliationStatus.MATCHED) {
                    discrepancyCount++;

                    publishEvent(
                            jobId,
                            orgId,
                            SettlementJobEventType.DISCREPANCY_DETECTED,
                            processedCount,
                            discrepancyCount,
                            "Discrepancy detected for transaction "
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
                    "Settlement job completed successfully"
            );

            log.info(
                    "Settlement job completed: jobId={}, orgId={}, processed={}, discrepancies={}",
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

            log.error(
                    "Settlement job failed: jobId={}, orgId={}, processed={}",
                    jobId,
                    orgId,
                    processedCount,
                    exception
            );

            publishEvent(
                    jobId,
                    orgId,
                    SettlementJobEventType.FAILED,
                    processedCount,
                    discrepancyCount,
                    "Settlement job failed: " + exception.getMessage()
            );

            throw exception;
        }
    }

    private void validateInput(
            String jobId,
            Long orgId,
            List<Long> transactionIds) {

        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException(
                    "Settlement job ID is required"
            );
        }

        if (orgId == null) {
            throw new IllegalArgumentException(
                    "Organization ID is required"
            );
        }

        if (transactionIds == null || transactionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transaction IDs must not be null or empty"
            );
        }

        if (transactionIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException(
                    "Transaction IDs must be positive and must not contain null"
            );
        }

        Set<Long> uniqueIds = new HashSet<>(transactionIds);

        if (uniqueIds.size() != transactionIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate transaction IDs are not allowed"
            );
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

        eventProducer.publish(event);
    }

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

