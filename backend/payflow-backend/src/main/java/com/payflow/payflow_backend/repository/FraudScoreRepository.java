package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.FraudScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudScoreRepository
        extends JpaRepository<FraudScore, Long> {

    List<FraudScore> findByTransactionId(Long transactionId);
}