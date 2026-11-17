package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.SettlementJobTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementJobTransactionRepository
        extends JpaRepository<SettlementJobTransaction, Long> {

    List<SettlementJobTransaction> findBySettlementJobIdOrderByCreatedAtAsc(
            Long settlementJobId
    );

    List<SettlementJobTransaction> findBySettlementJobId(
            Long settlementJobId
    );

    boolean existsBySettlementJobIdAndTransactionId(
            Long settlementJobId,
            Long transactionId
    );

    long countBySettlementJobId(Long settlementJobId);
}
