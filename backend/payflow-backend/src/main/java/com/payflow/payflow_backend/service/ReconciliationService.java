package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final ExternalPaymentRecordRepository externalPaymentRecordRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;

    public ReconciliationService(
            TransactionRepository transactionRepository,
            ExternalPaymentRecordRepository externalPaymentRecordRepository,
            ReconciliationRecordRepository reconciliationRecordRepository) {

        this.transactionRepository = transactionRepository;
        this.externalPaymentRecordRepository =
                externalPaymentRecordRepository;
        this.reconciliationRecordRepository =
                reconciliationRecordRepository;
    }

    @Transactional
    public ReconciliationRecord reconcileTransaction(Long transactionId) {

        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found: " + transactionId));

        ExternalPaymentRecord externalRecord =
                externalPaymentRecordRepository
                        .findByInternalTransactionId(transactionId)
                        .orElse(null);

        /*
         * No external record found.
         */
        if (externalRecord == null) {

            ReconciliationRecord record =
                    new ReconciliationRecord();

            record.setTransactionId(transaction.getId());

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

            return reconciliationRecordRepository.save(record);
        }

        /*
         * Compare internal and external records.
         */
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

        /*
         * Everything matches.
         */
        if (amountMatches
                && currencyMatches
                && statusMatches) {

            status = ReconciliationStatus.MATCHED;
        }

        /*
         * At least one field does not match.
         */
        else {

            status = ReconciliationStatus.MISMATCH;

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

        /*
         * Create reconciliation record.
         */
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

        return reconciliationRecordRepository.save(record);
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

        if (externalRecord.getInternalTransactionId() != null) {

            transaction = transactionRepository
                    .findById(
                            externalRecord.getInternalTransactionId())
                    .orElse(null);
        }

        /*
         * External payment exists,
         * but the corresponding internal transaction
         * does not exist.
         */
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

            return reconciliationRecordRepository.save(record);
        }

        /*
         * Internal transaction exists.
         * Reuse the normal reconciliation logic.
         */
        return reconcileTransaction(
                transaction.getId());
    }
    public java.util.Optional<ReconciliationRecord>
getLatestReconciliation(Long transactionId) {

    return reconciliationRecordRepository
            .findTopByTransactionIdOrderByReconciledAtDesc(
                    transactionId);
}

public java.util.List<ReconciliationRecord>
getByStatus(ReconciliationStatus status) {

    return reconciliationRecordRepository
            .findByStatusOrderByReconciledAtDesc(status);
}
}
