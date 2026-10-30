package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestigationRepository
        extends JpaRepository<Investigation, Long> {

    // Existing queries

    List<Investigation> findByStatusOrderByCreatedAtDesc(
            InvestigationStatus status
    );

    List<Investigation> findByPriorityOrderByCreatedAtDesc(
            InvestigationPriority priority
    );

    List<Investigation> findByTransactionIdOrderByCreatedAtDesc(
            Long transactionId
    );

    Optional<Investigation> findTopByTransactionIdOrderByCreatedAtDesc(
            Long transactionId
    );

    Optional<Investigation> findTopByReconciliationRecordIdOrderByCreatedAtDesc(
            Long reconciliationRecordId
    );

    // Tenant-scoped queries

    Optional<Investigation> findByIdAndOrgId(
            Long id,
            Long orgId
    );

    List<Investigation> findByOrgIdAndStatusOrderByCreatedAtDesc(
            Long orgId,
            InvestigationStatus status
    );

    List<Investigation> findByOrgIdAndPriorityOrderByCreatedAtDesc(
            Long orgId,
            InvestigationPriority priority
    );

    List<Investigation> findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
            Long orgId,
            Long transactionId
    );

    Optional<Investigation> findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
            Long orgId,
            Long transactionId
    );

    Optional<Investigation> findTopByOrgIdAndReconciliationRecordIdOrderByCreatedAtDesc(
            Long orgId,
            Long reconciliationRecordId
    );
}

