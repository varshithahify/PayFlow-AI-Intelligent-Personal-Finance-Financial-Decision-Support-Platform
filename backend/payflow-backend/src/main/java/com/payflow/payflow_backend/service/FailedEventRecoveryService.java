package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.event.TransactionEvent;
import com.payflow.payflow_backend.repository.FailedEventRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FailedEventRecoveryService {

    private final FailedEventRecordRepository failedEventRecordRepository;
    private final TransactionRepository transactionRepository;
    private final InvestigationService investigationService;

    public FailedEventRecoveryService(
            FailedEventRecordRepository failedEventRecordRepository,
            TransactionRepository transactionRepository,
            InvestigationService investigationService) {

        this.failedEventRecordRepository = failedEventRecordRepository;
        this.transactionRepository = transactionRepository;
        this.investigationService = investigationService;
    }

    @Transactional
    public boolean recordFailedEvent(
            TransactionEvent event,
            String failureReason) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "Failed transaction event must not be null");
        }

        UUID eventId = event.getEventId();

        if (eventId == null) {
            throw new IllegalArgumentException(
                    "Failed transaction event must have an event ID");
        }

        Long transactionId = event.getTransactionId();
        Long existingTransactionId = null;
        Long orgId = null;

        /*
         * Associate the failure with a transaction only
         * if that transaction actually exists in the database.
         */
        if (transactionId != null) {
            Transaction transaction = transactionRepository
                    .findById(transactionId)
                    .orElse(null);

            if (transaction != null) {
                existingTransactionId = transaction.getId();
                orgId = transaction.getOrgId();
            }
        }

        /*
         * Limit the failure reason to the database column length.
         */
        String safeFailureReason = failureReason;

        if (safeFailureReason != null
                && safeFailureReason.length() > 1000) {
            safeFailureReason = safeFailureReason.substring(0, 1000);
        }

        /*
         * Insert the failure record only once per event ID.
         * Duplicate events will not create duplicate investigations.
         */
        int inserted = failedEventRecordRepository.insertIfNotExists(
                eventId,
                existingTransactionId,
                orgId,
                event.getEventType() == null
                        ? "UNKNOWN"
                        : event.getEventType().name(),
                safeFailureReason,
                LocalDateTime.now());

        if (inserted == 0) {
            return false;
        }

        /*
         * Create an investigation only when the transaction
         * and its organization have been identified.
         */
        if (existingTransactionId != null && orgId != null) {

            String summary = "Kafka event processing failed. Event ID: "
                    + eventId
                    + ". Reason: "
                    + (safeFailureReason == null
                    ? "No failure details available"
                    : safeFailureReason);

            investigationService.createInvestigation(
                    existingTransactionId,
                    orgId,
                    null,
                    InvestigationPriority.CRITICAL,
                    InvestigationIssueType.PROCESSING_FAILURE,
                    summary);
        }

        return true;
    }
}