package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestigationRepository
        extends JpaRepository<Investigation, Long> {

    List<Investigation> findByStatusOrderByCreatedAtDesc(
            InvestigationStatus status);

    List<Investigation> findByPriorityOrderByCreatedAtDesc(
            InvestigationPriority priority);

    List<Investigation> findByTransactionIdOrderByCreatedAtDesc(
            Long transactionId);

    Optional<Investigation>
    findTopByTransactionIdOrderByCreatedAtDesc(
            Long transactionId);

    Optional<Investigation>
    findTopByReconciliationRecordIdOrderByCreatedAtDesc(
            Long reconciliationRecordId);
}