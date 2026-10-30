package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.AIInvestigationFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AIInvestigationFindingRepository
        extends JpaRepository<AIInvestigationFinding, Long> {

    List<AIInvestigationFinding>
    findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
            Long orgId,
            Long investigationId
    );

    Optional<AIInvestigationFinding>
    findTopByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
            Long orgId,
            Long investigationId
    );

    List<AIInvestigationFinding>
    findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
            Long orgId,
            Long transactionId
    );
}

