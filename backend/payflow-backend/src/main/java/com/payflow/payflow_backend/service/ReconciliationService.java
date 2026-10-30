package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;
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

        this.transactionRepository = transactionRepository;
        this.externalPaymentRecordRepository = externalPaymentRecordRepository;
        this.reconciliationRecordRepository = reconciliationRecordRepository;
        this.investigationRepository = investigationRepository;
        this.investigationService = investigationService;
    }

    /**
     * Reconciles a transaction for an authenticated request or a legacy
     * internal caller.
     *
     * If a tenant context exists, transaction ownership must match it.
     * Background workflows should use the explicit organization-scoped
     * overload below.
     */
    @Transactional
    public ReconciliationRecord reconcileTransaction(Long transactionId) {

        Long orgId = TenantContext.getOrgId();

        if (orgId == null) {
            throw new IllegalStateException(
                    "Organization context is required. "
                            + "Use the organization-scoped method for trusted background workflows.");
        }

        return reconcileTransaction(transactionId, orgId);
    }

    /**
     * Organization-scoped reconciliation for trusted internal workflows.
     * The organization ID must come from validated authentication or
     * trusted event/job context, and is checked against the database.
     */
    @Transactional
    public ReconciliationRecord reconcileTransaction(
            Long transactionId,
            Long expectedOrgId) {

        Transaction transaction = getVerifiedTransaction(
                transactionId,
                expectedOrgId);

        ExternalPaymentRecord externalRecord =
                externalPaymentRecordRepository
                        .findByInternalTransactionId(transactionId)
                        .orElse(null);

        if (externalRecord == null) {

            ReconciliationRecord record = new ReconciliationRecord();
            record.setTransactionId(transaction.getId());
            record.setOrgId(transaction.getOrgId());
            record.setStatus(ReconciliationStatus.MISSING_EXTERNAL);
            record.setInternalAmount(transaction.getAmount());
            record.setInternalStatus(transaction.getStatus().name());
            record.setMismatchReason(
                    "No matching external payment record found");
            record.setReconciledAt(LocalDateTime.now());

            ReconciliationRecord savedRecord =
                    reconciliationRecordRepository.save(record);

            createInvestigationIfNeeded(
                    savedRecord,
                    InvestigationIssueType.MISSING_EXTERNAL);

            return savedRecord;
        }

        boolean amountMatches =
                transaction.getAmount()
                        .compareTo(externalRecord.getAmount()) == 0;

        boolean currencyMatches =
                transaction.getCurrency()
                        .equalsIgnoreCase(externalRecord.getCurrency());

        boolean statusMatches =
                transaction.getStatus()
                        .name()
                        .equalsIgnoreCase(externalRecord.getStatus());

        ReconciliationStatus status;
        String mismatchReason = null;

        if (amountMatches && currencyMatches && statusMatches) {
            status = ReconciliationStatus.MATCHED;
        } else {
            status = ReconciliationStatus.MISMATCH;

            StringBuilder reason = new StringBuilder();

            if (!amountMatches) {
                reason.append("Amount mismatch. ");
            }

            if (!currencyMatches) {
                reason.append("Currency mismatch. ");
            }

            if (!statusMatches) {
                reason.append("Status mismatch. ");
            }

            mismatchReason = reason.toString().trim();
        }

        ReconciliationRecord record = new ReconciliationRecord();
        record.setTransactionId(transaction.getId());
        record.setOrgId(transaction.getOrgId());
        record.setExternalRecordId(externalRecord.getId());
        record.setStatus(status);
        record.setInternalAmount(transaction.getAmount());
        record.setExternalAmount(externalRecord.getAmount());
        record.setInternalStatus(transaction.getStatus().name());
        record.setExternalStatus(externalRecord.getStatus());
        record.setMismatchReason(mismatchReason);
        record.setReconciledAt(LocalDateTime.now());

        ReconciliationRecord savedRecord =
                reconciliationRecordRepository.save(record);

        if (status == ReconciliationStatus.MISMATCH) {
            InvestigationIssueType issueType =
                    determineMismatchIssueType(
                            amountMatches,
                            currencyMatches,
                            statusMatches);

            createInvestigationIfNeeded(savedRecord, issueType);
        }

        return savedRecord;
    }

    /**
     * Reconciles an external record only if its linked internal transaction
     * exists and belongs to the expected organization.
     *
     * Unmatched external records are not assigned to a tenant here because
     * ExternalPaymentRecord currently has no verified organization owner.
     */
    @Transactional
    public ReconciliationRecord reconcileExternalRecord(
            Long externalRecordId,
            Long expectedOrgId) {

        if (externalRecordId == null || expectedOrgId == null) {
            throw new IllegalArgumentException(
                    "External record ID and organization ID are required");
        }

        ExternalPaymentRecord externalRecord =
                externalPaymentRecordRepository
                        .findById(externalRecordId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "External payment record not found"));

        Long transactionId = externalRecord.getInternalTransactionId();

        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Cannot reconcile an unmatched external record "
                            + "without verified organization ownership");
        }

        Transaction transaction =
                getVerifiedTransaction(transactionId, expectedOrgId);

        return reconcileTransaction(transaction.getId(), expectedOrgId);
    }

    /**
     * Compatibility overload for callers that already establish the
     * authenticated tenant context.
     */
    @Transactional
    public ReconciliationRecord reconcileExternalRecord(
            Long externalRecordId) {

        return reconcileExternalRecord(
                externalRecordId,
                TenantContext.requireOrgId());
    }

    private Transaction getVerifiedTransaction(
            Long transactionId,
            Long expectedOrgId) {

        if (transactionId == null || expectedOrgId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID and organization ID are required");
        }

        return transactionRepository
                .findByIdAndOrgId(transactionId, expectedOrgId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found"));
    }

    private void createInvestigationIfNeeded(
            ReconciliationRecord reconciliationRecord,
            InvestigationIssueType issueType) {

        Long transactionId = reconciliationRecord.getTransactionId();
        Long orgId = reconciliationRecord.getOrgId();

        if (transactionId == null || orgId == null) {
            // Do not create a tenant-owned investigation without a verified
            // internal transaction and organization.
            return;
        }

        Optional<Investigation> existingInvestigation =
                investigationRepository
                        .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                                orgId,
                                transactionId);

        if (existingInvestigation.isPresent()) {
            Investigation existing = existingInvestigation.get();

            if (existing.getIssueType() == issueType
                    && existing.getStatus() != InvestigationStatus.RESOLVED) {
                return;
            }
        }

        String summary = reconciliationRecord.getMismatchReason();

        if (summary == null || summary.isBlank()) {
            summary = "Reconciliation issue detected: " + issueType;
        }

        investigationService.createInvestigation(
                transactionId,
                orgId,
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

    @Transactional(readOnly = true)
    public Optional<ReconciliationRecord> getLatestReconciliation(
            Long transactionId) {

        Long orgId = TenantContext.requireOrgId();

        getVerifiedTransaction(transactionId, orgId);

        return reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        orgId,
                        transactionId);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRecord> getByStatus(
            ReconciliationStatus status) {

        Long orgId = TenantContext.requireOrgId();

        return reconciliationRecordRepository
                .findByOrgIdAndStatusOrderByReconciledAtDesc(
                        orgId,
                        status);
    }
}

