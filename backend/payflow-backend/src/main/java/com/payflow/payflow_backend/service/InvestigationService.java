package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvestigationService {

    private final InvestigationRepository investigationRepository;

    public InvestigationService(
            InvestigationRepository investigationRepository) {

        this.investigationRepository =
                investigationRepository;
    }

    @Transactional
    public Investigation createInvestigation(
            Long transactionId,
            Long reconciliationRecordId,
            InvestigationPriority priority,
            com.payflow.payflow_backend.entity.InvestigationIssueType issueType,
            String summary) {

        Investigation investigation =
                new Investigation();

        investigation.setTransactionId(transactionId);
        investigation.setReconciliationRecordId(
                reconciliationRecordId);
        investigation.setStatus(
                InvestigationStatus.OPEN);
        investigation.setPriority(priority);
        investigation.setIssueType(issueType);
        investigation.setSummary(summary);

        LocalDateTime now =
                LocalDateTime.now();

        investigation.setCreatedAt(now);
        investigation.setUpdatedAt(now);

        return investigationRepository.save(
                investigation);
    }

    @Transactional(readOnly = true)
    public Investigation getInvestigation(Long id) {

        return investigationRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Investigation not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Investigation> getByStatus(
            InvestigationStatus status) {

        return investigationRepository
                .findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public List<Investigation> getByPriority(
            InvestigationPriority priority) {

        return investigationRepository
                .findByPriorityOrderByCreatedAtDesc(priority);
    }

    @Transactional(readOnly = true)
    public List<Investigation> getByTransactionId(
            Long transactionId) {

        return investigationRepository
                .findByTransactionIdOrderByCreatedAtDesc(
                        transactionId);
    }

    @Transactional
    public Investigation updateStatus(
            Long id,
            InvestigationStatus newStatus) {

        Investigation investigation =
                getInvestigation(id);

        validateStatusTransition(
                investigation.getStatus(),
                newStatus);

        investigation.setStatus(newStatus);
        investigation.setUpdatedAt(
                LocalDateTime.now());

        if (newStatus == InvestigationStatus.RESOLVED) {
            investigation.setResolvedAt(
                    LocalDateTime.now());
        }

        return investigationRepository.save(
                investigation);
    }

    private void validateStatusTransition(
            InvestigationStatus currentStatus,
            InvestigationStatus newStatus) {

        if (currentStatus == newStatus) {
            throw new IllegalStateException(
                    "Investigation is already in status: "
                            + currentStatus);
        }

        boolean validTransition =
                switch (currentStatus) {

                    case OPEN ->
                            newStatus ==
                                    InvestigationStatus.INVESTIGATING
                                    || newStatus ==
                                    InvestigationStatus.ESCALATED;

                    case INVESTIGATING ->
                            newStatus ==
                                    InvestigationStatus.RESOLVED
                                    || newStatus ==
                                    InvestigationStatus.ESCALATED;

                    case ESCALATED ->
                            newStatus ==
                                    InvestigationStatus.INVESTIGATING
                                    || newStatus ==
                                    InvestigationStatus.RESOLVED;

                    case RESOLVED ->
                            false;
                };

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid investigation status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus);
        }
    }
}