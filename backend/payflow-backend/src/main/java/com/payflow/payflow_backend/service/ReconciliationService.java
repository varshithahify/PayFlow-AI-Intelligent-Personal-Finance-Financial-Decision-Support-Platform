package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final ExternalPaymentRecordRepository externalPaymentRecordRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final InvestigationRepository investigationRepository;
    private final InvestigationService investigationService;

    public ReconciliationService(
            TransactionRepository transactionRepository,
            ExternalPaymentRecordRepository externalPaymentRecordRepository,
            ReconciliationRecordRepository reconciliationRecordRepository,
            InvestigationRepository investigationRepository,
            InvestigationService investigationService) {

        this.transactionRepository =
                transactionRepository;

        this.externalPaymentRecordRepository =
                externalPaymentRecordRepository;

        this.reconciliationRecordRepository =
                reconciliationRecordRepository;

        this.investigationRepository =
                investigationRepository;

        this.investigationService =
                investigationService;
    }

    @Transactional
    public ReconciliationRecord reconcileTransaction(
            Long transactionId) {

        Transaction transaction =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Transaction not found: "
                                                + transactionId));

        ExternalPaymentRecord externalRecord =
                externalPaymentRecordRepository
                        .findByInternalTransactionId(transactionId)
                        .orElse(null);

        if (externalRecord == null) {

            ReconciliationRecord record =
                    new ReconciliationRecord();

            record.setTransactionId(
                    transaction.getId());

            record.setStatus(
                    ReconciliationStatus.MISSING_EXTERNAL);

            record.setInternalAmount(
                    transaction.getAmount());

            record.setInternalStatus(
                    transaction.getStatus().name());

            record.setMismatchReason(
                    "No matching external payment record found");

            record.setReconciledAt(
                    LocalDateTime.now());

            ReconciliationRecord savedRecord =
                    reconciliationRecordRepository.save(record);

            createInvestigationIfNeeded(
                    savedRecord,
                    InvestigationIssueType.MISSING_EXTERNAL);

            return savedRecord;
        }

        boolean amountMatches =
                transaction.getAmount()
                        .compareTo(
                                externalRecord.getAmount()) == 0;

        boolean currencyMatches =
                transaction.getCurrency()
                        .equalsIgnoreCase(
                                externalRecord.getCurrency());

        boolean statusMatches =
                transaction.getStatus()
                        .name()
                        .equalsIgnoreCase(
                                externalRecord.getStatus());

        ReconciliationStatus status;
        String mismatchReason = null;

        if (amountMatches
                && currencyMatches
                && statusMatches) {

            status =
                    ReconciliationStatus.MATCHED;

        } else {

            status =
                    ReconciliationStatus.MISMATCH;

            StringBuilder reason =
                    new StringBuilder();

            if (!amountMatches) {
                reason.append(
                        "Amount mismatch. ");
            }

            if (!currencyMatches) {
                reason.append(
                        "Currency mismatch. ");
            }

            if (!statusMatches) {
                reason.append(
                        "Status mismatch. ");
            }

            mismatchReason =
                    reason.toString().trim();
        }

        ReconciliationRecord record =
                new ReconciliationRecord();

        record.setTransactionId(
                transaction.getId());

        record.setExternalRecordId(
                externalRecord.getId());

        record.setStatus(status);

        record.setInternalAmount(
                transaction.getAmount());

        record.setExternalAmount(
                externalRecord.getAmount());

        record.setInternalStatus(
                transaction.getStatus().name());

        record.setExternalStatus(
                externalRecord.getStatus());

        record.setMismatchReason(
                mismatchReason);

        record.setReconciledAt(
                LocalDateTime.now());

        ReconciliationRecord savedRecord =
                reconciliationRecordRepository.save(record);

        if (status == ReconciliationStatus.MISMATCH) {

            InvestigationIssueType issueType =
                    determineMismatchIssueType(
                            amountMatches,
                            currencyMatches,
                            statusMatches);

            createInvestigationIfNeeded(
                    savedRecord,
                    issueType);
        }

        return savedRecord;
    }

    @Transactional
    public ReconciliationRecord reconcileExternalRecord(
            Long externalRecordId) {

        ExternalPaymentRecord externalRecord =
                externalPaymentRecordRepository
                        .findById(externalRecordId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "External payment record not found: "
                                                + externalRecordId));

        Transaction transaction = null;

        if (externalRecord.getInternalTransactionId()
                != null) {

            transaction =
                    transactionRepository
                            .findById(
                                    externalRecord
                                            .getInternalTransactionId())
                            .orElse(null);
        }

        if (transaction == null) {

            ReconciliationRecord record =
                    new ReconciliationRecord();

            record.setTransactionId(null);

            record.setExternalRecordId(
                    externalRecord.getId());

            record.setStatus(
                    ReconciliationStatus.MISSING_INTERNAL);

            record.setExternalAmount(
                    externalRecord.getAmount());

            record.setExternalStatus(
                    externalRecord.getStatus());

            record.setMismatchReason(
                    "No matching internal transaction found");

            record.setReconciledAt(
                    LocalDateTime.now());

            ReconciliationRecord savedRecord =
                    reconciliationRecordRepository.save(record);

            createInvestigationIfNeeded(
                    savedRecord,
                    InvestigationIssueType.MISSING_INTERNAL);

            return savedRecord;
        }

        return reconcileTransaction(
                transaction.getId());
    }

    private void createInvestigationIfNeeded(
            ReconciliationRecord reconciliationRecord,
            InvestigationIssueType issueType) {

        Optional<Investigation> existingInvestigation =
                investigationRepository
                        .findTopByTransactionIdOrderByCreatedAtDesc(
                                reconciliationRecord
                                        .getTransactionId());

        if (existingInvestigation.isPresent()) {

            Investigation existing =
                    existingInvestigation.get();

            if (existing.getIssueType() == issueType
                    && existing.getStatus()
                    != com.payflow.payflow_backend.entity.InvestigationStatus.RESOLVED) {

                return;
            }
        }

        String summary =
                reconciliationRecord.getMismatchReason();

        if (summary == null
                || summary.isBlank()) {

            summary =
                    "Reconciliation issue detected: "
                            + issueType;
        }

        investigationService.createInvestigation(
                reconciliationRecord.getTransactionId(),
                reconciliationRecord.getId(),
                InvestigationPriority.HIGH,
                issueType,
                summary);
    }

    private InvestigationIssueType determineMismatchIssueType(
            boolean amountMatches,
            boolean currencyMatches,
            boolean statusMatches) {

        if (!amountMatches) {
            return InvestigationIssueType.AMOUNT_MISMATCH;
        }

        if (!currencyMatches) {
            return InvestigationIssueType.CURRENCY_MISMATCH;
        }

        return InvestigationIssueType.STATUS_MISMATCH;
    }

    public Optional<ReconciliationRecord>
    getLatestReconciliation(Long transactionId) {

        return reconciliationRecordRepository
                .findTopByTransactionIdOrderByReconciledAtDesc(
                        transactionId);
    }

    public List<ReconciliationRecord>
    getByStatus(ReconciliationStatus status) {

        return reconciliationRecordRepository
                .findByStatusOrderByReconciledAtDesc(
                        status);
    }
}