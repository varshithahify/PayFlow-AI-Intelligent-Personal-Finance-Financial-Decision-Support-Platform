package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationRecordRepository
        extends JpaRepository<ReconciliationRecord, Long> {

    // Existing queries — retain for compatibility with existing workflows.
    Optional<ReconciliationRecord>
    findTopByTransactionIdOrderByReconciledAtDesc(
            Long transactionId);

    List<ReconciliationRecord>
    findByStatusOrderByReconciledAtDesc(
            ReconciliationStatus status);

    // Tenant-scoped queries.
    Optional<ReconciliationRecord>
    findByIdAndOrgId(
            Long id,
            Long orgId);

    Optional<ReconciliationRecord>
    findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
            Long orgId,
            Long transactionId);

    List<ReconciliationRecord>
    findByOrgIdAndStatusOrderByReconciledAtDesc(
            Long orgId,
            ReconciliationStatus status);

    Optional<ReconciliationRecord>
    findByIdAndOrgIdAndTransactionId(
            Long id,
            Long orgId,
            Long transactionId);

    Optional<ReconciliationRecord>
    findByIdAndOrgIdAndExternalRecordId(
            Long id,
            Long orgId,
            Long externalRecordId);
}

