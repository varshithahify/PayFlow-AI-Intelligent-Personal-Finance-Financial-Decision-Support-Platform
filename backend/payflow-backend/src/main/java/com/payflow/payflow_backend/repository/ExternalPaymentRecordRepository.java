package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalPaymentRecordRepository
        extends JpaRepository<ExternalPaymentRecord, Long> {

    Optional<ExternalPaymentRecord>
    findByInternalTransactionId(Long internalTransactionId);

    Optional<ExternalPaymentRecord>
    findByExternalTransactionId(String externalTransactionId);
}