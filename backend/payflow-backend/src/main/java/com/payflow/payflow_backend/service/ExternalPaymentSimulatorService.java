package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ExternalPaymentSimulatorService {

    private final ExternalPaymentRecordRepository externalPaymentRecordRepository;

    public ExternalPaymentSimulatorService(
            ExternalPaymentRecordRepository externalPaymentRecordRepository) {
        this.externalPaymentRecordRepository =
                externalPaymentRecordRepository;
    }

    public ExternalPaymentRecord createExternalRecord(
            Transaction transaction,
            String gateway) {

        String externalTransactionId =
                "EXT-" + UUID.randomUUID();

        ExternalPaymentRecord externalRecord =
                new ExternalPaymentRecord(
                        externalTransactionId,
                        transaction.getId(),
                        transaction.getAmount(),
                        transaction.getCurrency(),
                        transaction.getStatus().name(),
                        gateway,
                        LocalDateTime.now()
                );

        return externalPaymentRecordRepository.save(externalRecord);
    }
}