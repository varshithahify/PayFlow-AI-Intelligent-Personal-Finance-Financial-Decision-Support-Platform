package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.SettlementJobExecution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementJobExecutionRepository
        extends JpaRepository<SettlementJobExecution, Long> {

    List<SettlementJobExecution> findBySettlementJobIdOrderByAttemptNumberDesc(
            Long settlementJobId
    );

    Optional<SettlementJobExecution>
    findTopBySettlementJobIdOrderByAttemptNumberDesc(
            Long settlementJobId
    );

    Optional<SettlementJobExecution> findBySettlementJobIdAndAttemptNumber(
            Long settlementJobId,
            Integer attemptNumber
    );

    long countBySettlementJobId(Long settlementJobId);
}
