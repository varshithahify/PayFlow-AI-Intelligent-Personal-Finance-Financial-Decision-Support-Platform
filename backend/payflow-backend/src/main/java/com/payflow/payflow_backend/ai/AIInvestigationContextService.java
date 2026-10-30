package com.payflow.payflow_backend.ai;

import com.payflow.payflow_backend.ai.dto.AIInvestigationContext;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AIInvestigationContextService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final InvestigationRepository investigationRepository;

    public AIInvestigationContextService(
            TransactionRepository transactionRepository,
            ReconciliationRecordRepository reconciliationRecordRepository,
            InvestigationRepository investigationRepository) {

        this.transactionRepository = transactionRepository;
        this.reconciliationRecordRepository = reconciliationRecordRepository;
        this.investigationRepository = investigationRepository;
    }

    @Transactional(readOnly = true)
    public AIInvestigationContext buildContext(Long transactionId) {

        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }

        // Step 1: Obtain the authenticated organization's ID.
        Long orgId = TenantContext.requireOrgId();

        // Step 2: Retrieve only a transaction belonging to this organization.
        Transaction transaction = transactionRepository
                .findByIdAndOrgId(transactionId, orgId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found"));

        // Step 3: Retrieve the latest reconciliation record for this tenant.
        ReconciliationRecord reconciliation = reconciliationRecordRepository
                .findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        orgId,
                        transaction.getId())
                .orElse(null);

        // Step 4: Retrieve the latest investigation for this tenant.
        Investigation investigation = investigationRepository
                .findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        orgId,
                        transaction.getId())
                .orElse(null);

        // Step 5: Build a context using the verified database records.
        return new AIInvestigationContext(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getPaymentMethod(),
                transaction.getStatus().name(),
                transaction.getGatewayName(),
                transaction.getCreatedAt(),

                reconciliation != null ? reconciliation.getId() : null,
                reconciliation != null
                        ? reconciliation.getStatus().name() : null,
                reconciliation != null
                        ? reconciliation.getInternalAmount() : null,
                reconciliation != null
                        ? reconciliation.getExternalAmount() : null,
                reconciliation != null
                        ? reconciliation.getInternalStatus() : null,
                reconciliation != null
                        ? reconciliation.getExternalStatus() : null,
                reconciliation != null
                        ? reconciliation.getMismatchReason() : null,
                reconciliation != null
                        ? reconciliation.getReconciledAt() : null,

                investigation != null ? investigation.getId() : null,
                investigation != null
                        ? investigation.getIssueType().name() : null,
                investigation != null
                        ? investigation.getPriority().name() : null,
                investigation != null
                        ? investigation.getStatus().name() : null,
                investigation != null ? investigation.getSummary() : null
        );
    }
}