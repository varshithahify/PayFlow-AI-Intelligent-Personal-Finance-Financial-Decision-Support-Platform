package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationRecordRepository
        extends JpaRepository<ReconciliationRecord, Long> {

    Optional<ReconciliationRecord>
    findTopByTransactionIdOrderByReconciledAtDesc(
            Long transactionId);

    List<ReconciliationRecord>
    findByStatusOrderByReconciledAtDesc(
            ReconciliationStatus status);
}