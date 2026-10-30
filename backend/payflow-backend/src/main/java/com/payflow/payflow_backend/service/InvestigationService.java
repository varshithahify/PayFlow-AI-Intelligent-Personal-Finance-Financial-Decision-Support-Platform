package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final TransactionRepository transactionRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;

    public InvestigationService(
            InvestigationRepository investigationRepository,
            TransactionRepository transactionRepository,
            ReconciliationRecordRepository reconciliationRecordRepository) {

        this.investigationRepository = investigationRepository;
        this.transactionRepository = transactionRepository;
        this.reconciliationRecordRepository = reconciliationRecordRepository;
    }

    /**
     * Creates an investigation using the authenticated tenant context.
     */
    @Transactional
    public Investigation createInvestigation(
            Long transactionId,
            Long reconciliationRecordId,
            InvestigationPriority priority,
            InvestigationIssueType issueType,
            String summary) {

        Long orgId = TenantContext.requireOrgId();

        return createInvestigation(
                transactionId,
                orgId,
                reconciliationRecordId,
                priority,
                issueType,
                summary);
    }

    /**
     * Creates an investigation for a verified organization.
     *
     * The transaction must belong to the supplied organization.
     * If a reconciliation record ID is supplied, that record must
     * belong to the same organization and transaction.
     */
    @Transactional
    public Investigation createInvestigation(
            Long transactionId,
            Long orgId,
            Long reconciliationRecordId,
            InvestigationPriority priority,
            InvestigationIssueType issueType,
            String summary) {

        if (transactionId == null || orgId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID and organization ID are required");
        }

        if (priority == null || issueType == null) {
            throw new IllegalArgumentException(
                    "Investigation priority and issue type are required");
        }

        // Prevent a caller from supplying a different tenant.
        Long currentOrgId = TenantContext.getOrgId();

        if (currentOrgId != null && !currentOrgId.equals(orgId)) {
            throw new IllegalArgumentException(
                    "Organization does not match the current tenant");
        }

        // Verify transaction ownership directly in the database query.
        Transaction transaction = transactionRepository
                .findByIdAndOrgId(transactionId, orgId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found"));

        // Verify reconciliation ownership when a record ID is supplied.
        if (reconciliationRecordId != null) {

            ReconciliationRecord reconciliationRecord =
                    reconciliationRecordRepository
                            .findByIdAndOrgIdAndTransactionId(
                                    reconciliationRecordId,
                                    orgId,
                                    transaction.getId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Reconciliation record not found"));

            // Additional defensive ownership checks.
            if (!orgId.equals(reconciliationRecord.getOrgId())
                    || !transaction.getId().equals(
                            reconciliationRecord.getTransactionId())) {

                throw new IllegalArgumentException(
                        "Reconciliation record not found");
            }
        }

        Investigation investigation = new Investigation();

        investigation.setTransactionId(transaction.getId());
        investigation.setOrgId(orgId);
        investigation.setReconciliationRecordId(reconciliationRecordId);
        investigation.setStatus(InvestigationStatus.OPEN);
        investigation.setPriority(priority);
        investigation.setIssueType(issueType);
        investigation.setSummary(summary);

        LocalDateTime now = LocalDateTime.now();

        investigation.setCreatedAt(now);
        investigation.setUpdatedAt(now);

        return investigationRepository.save(investigation);
    }

    /**
     * Retrieves an investigation belonging to the authenticated organization.
     */
    @Transactional(readOnly = true)
    public Investigation getInvestigation(Long id) {

        Long orgId = TenantContext.requireOrgId();

        return investigationRepository
                .findByIdAndOrgId(id, orgId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Investigation not found"));
    }

    @Transactional(readOnly = true)
    public List<Investigation> getByStatus(
            InvestigationStatus status) {

        Long orgId = TenantContext.requireOrgId();

        return investigationRepository
                .findByOrgIdAndStatusOrderByCreatedAtDesc(
                        orgId,
                        status);
    }

    @Transactional(readOnly = true)
    public List<Investigation> getByPriority(
            InvestigationPriority priority) {

        Long orgId = TenantContext.requireOrgId();

        return investigationRepository
                .findByOrgIdAndPriorityOrderByCreatedAtDesc(
                        orgId,
                        priority);
    }

    @Transactional(readOnly = true)
    public List<Investigation> getByTransactionId(
            Long transactionId) {

        Long orgId = TenantContext.requireOrgId();

        // Verify transaction ownership before returning investigations.
        transactionRepository
                .findByIdAndOrgId(transactionId, orgId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Transaction not found"));

        return investigationRepository
                .findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        orgId,
                        transactionId);
    }

    @Transactional
    public Investigation updateStatus(
            Long id,
            InvestigationStatus newStatus) {

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "New investigation status is required");
        }

        Investigation investigation = getInvestigation(id);

        validateStatusTransition(
                investigation.getStatus(),
                newStatus);

        investigation.setStatus(newStatus);

        LocalDateTime now = LocalDateTime.now();

        investigation.setUpdatedAt(now);

        if (newStatus == InvestigationStatus.RESOLVED) {
            investigation.setResolvedAt(now);
        }

        return investigationRepository.save(investigation);
    }

    private void validateStatusTransition(
            InvestigationStatus currentStatus,
            InvestigationStatus newStatus) {

        if (currentStatus == newStatus) {
            throw new IllegalStateException(
                    "Investigation is already in status: "
                            + currentStatus);
        }

        boolean validTransition = switch (currentStatus) {

            case OPEN ->
                    newStatus == InvestigationStatus.INVESTIGATING
                            || newStatus == InvestigationStatus.ESCALATED;

            case INVESTIGATING ->
                    newStatus == InvestigationStatus.RESOLVED
                            || newStatus == InvestigationStatus.ESCALATED;

            case ESCALATED ->
                    newStatus == InvestigationStatus.INVESTIGATING
                            || newStatus == InvestigationStatus.RESOLVED;

            case RESOLVED -> false;
        };

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid investigation status transition: "
                            + currentStatus + " -> " + newStatus);
        }
    }
}

