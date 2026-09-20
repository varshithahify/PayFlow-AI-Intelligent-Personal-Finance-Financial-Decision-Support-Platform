package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;
import com.payflow.payflow_backend.repository.InvestigationRepository;
import com.payflow.payflow_backend.repository.ReconciliationRecordRepository;
import com.payflow.payflow_backend.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExternalPaymentRecordRepository externalPaymentRecordRepository;

    @Mock
    private ReconciliationRecordRepository reconciliationRecordRepository;

    @Mock
    private InvestigationRepository investigationRepository;

    @Mock
    private InvestigationService investigationService;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void shouldReturnMatchedWhenRecordsMatch() {

        Long transactionId = 7L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAmount(new BigDecimal("9999.00"));
        transaction.setCurrency("INR");
        transaction.setStatus(TransactionStatus.SUCCESS);

        ExternalPaymentRecord externalRecord =
                new ExternalPaymentRecord();

        externalRecord.setId(1L);
        externalRecord.setInternalTransactionId(transactionId);
        externalRecord.setAmount(new BigDecimal("9999.00"));
        externalRecord.setCurrency("INR");
        externalRecord.setStatus("SUCCESS");
        externalRecord.setGateway("GATEWAY_A");
        externalRecord.setProcessedAt(LocalDateTime.now());

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(externalPaymentRecordRepository
                .findByInternalTransactionId(transactionId))
                .thenReturn(Optional.of(externalRecord));

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> {

                    ReconciliationRecord record =
                            invocation.getArgument(0);

                    record.setId(100L);

                    return record;
                });

        ReconciliationRecord result =
                reconciliationService
                        .reconcileTransaction(transactionId);

        assertEquals(
                ReconciliationStatus.MATCHED,
                result.getStatus());

        assertEquals(
                transactionId,
                result.getTransactionId());

        assertEquals(
                new BigDecimal("9999.00"),
                result.getInternalAmount());

        assertEquals(
                new BigDecimal("9999.00"),
                result.getExternalAmount());

        assertNull(result.getMismatchReason());

        verify(investigationService, never())
                .createInvestigation(
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
    }

    @Test
    void shouldReturnMismatchWhenAmountDoesNotMatch() {

        Long transactionId = 7L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAmount(new BigDecimal("9999.00"));
        transaction.setCurrency("INR");
        transaction.setStatus(TransactionStatus.SUCCESS);

        ExternalPaymentRecord externalRecord =
                new ExternalPaymentRecord();

        externalRecord.setId(1L);
        externalRecord.setInternalTransactionId(transactionId);
        externalRecord.setAmount(new BigDecimal("8999.00"));
        externalRecord.setCurrency("INR");
        externalRecord.setStatus("SUCCESS");
        externalRecord.setGateway("GATEWAY_A");
        externalRecord.setProcessedAt(LocalDateTime.now());

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(externalPaymentRecordRepository
                .findByInternalTransactionId(transactionId))
                .thenReturn(Optional.of(externalRecord));

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> {

                    ReconciliationRecord record =
                            invocation.getArgument(0);

                    record.setId(100L);

                    return record;
                });

        when(investigationRepository
                .findTopByTransactionIdOrderByCreatedAtDesc(
                        transactionId))
                .thenReturn(Optional.empty());

        ReconciliationRecord result =
                reconciliationService
                        .reconcileTransaction(transactionId);

        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.getStatus());

        assertEquals(
                "Amount mismatch.",
                result.getMismatchReason());

        verify(investigationService)
                .createInvestigation(
                        eq(transactionId),
                        anyLong(),
                        eq(InvestigationPriority.HIGH),
                        eq(InvestigationIssueType.AMOUNT_MISMATCH),
                        eq("Amount mismatch."));
    }

    @Test
    void shouldReturnMissingExternalWhenExternalRecordDoesNotExist() {

        Long transactionId = 7L;

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAmount(new BigDecimal("9999.00"));
        transaction.setCurrency("INR");
        transaction.setStatus(TransactionStatus.SUCCESS);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(externalPaymentRecordRepository
                .findByInternalTransactionId(transactionId))
                .thenReturn(Optional.empty());

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> {

                    ReconciliationRecord record =
                            invocation.getArgument(0);

                    record.setId(100L);

                    return record;
                });

        when(investigationRepository
                .findTopByTransactionIdOrderByCreatedAtDesc(
                        transactionId))
                .thenReturn(Optional.empty());

        ReconciliationRecord result =
                reconciliationService
                        .reconcileTransaction(transactionId);

        assertEquals(
                ReconciliationStatus.MISSING_EXTERNAL,
                result.getStatus());

        assertEquals(
                transactionId,
                result.getTransactionId());

        assertEquals(
                new BigDecimal("9999.00"),
                result.getInternalAmount());

        assertNull(result.getExternalAmount());

        assertEquals(
                "No matching external payment record found",
                result.getMismatchReason());

        verify(investigationService)
                .createInvestigation(
                        eq(transactionId),
                        anyLong(),
                        eq(InvestigationPriority.HIGH),
                        eq(InvestigationIssueType.MISSING_EXTERNAL),
                        eq("No matching external payment record found"));
    }

    @Test
    void shouldReturnMissingInternalWhenExternalRecordHasNoInternalTransaction() {

        Long externalRecordId = 2L;

        ExternalPaymentRecord externalRecord =
                new ExternalPaymentRecord();

        externalRecord.setId(externalRecordId);
        externalRecord.setInternalTransactionId(null);
        externalRecord.setAmount(new BigDecimal("1500.00"));
        externalRecord.setCurrency("INR");
        externalRecord.setStatus("SUCCESS");
        externalRecord.setGateway("GATEWAY_B");
        externalRecord.setProcessedAt(LocalDateTime.now());

        when(externalPaymentRecordRepository
                .findById(externalRecordId))
                .thenReturn(Optional.of(externalRecord));

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> {

                    ReconciliationRecord record =
                            invocation.getArgument(0);

                    record.setId(100L);

                    return record;
                });

        when(investigationRepository
                .findTopByTransactionIdOrderByCreatedAtDesc(
                        isNull()))
                .thenReturn(Optional.empty());

        ReconciliationRecord result =
                reconciliationService
                        .reconcileExternalRecord(
                                externalRecordId);

        assertEquals(
                ReconciliationStatus.MISSING_INTERNAL,
                result.getStatus());

        assertNull(result.getTransactionId());

        assertEquals(
                externalRecordId,
                result.getExternalRecordId());

        assertEquals(
                new BigDecimal("1500.00"),
                result.getExternalAmount());

        assertEquals(
                "No matching internal transaction found",
                result.getMismatchReason());

        verify(investigationService)
                .createInvestigation(
                        isNull(),
                        anyLong(),
                        eq(InvestigationPriority.HIGH),
                        eq(InvestigationIssueType.MISSING_INTERNAL),
                        eq("No matching internal transaction found"));
    }

    @Test
    void shouldReturnLatestReconciliation() {

        Long transactionId = 7L;

        ReconciliationRecord record =
                new ReconciliationRecord();

        record.setId(100L);
        record.setTransactionId(transactionId);
        record.setStatus(
                ReconciliationStatus.MATCHED);

        when(reconciliationRecordRepository
                .findTopByTransactionIdOrderByReconciledAtDesc(
                        transactionId))
                .thenReturn(Optional.of(record));

        Optional<ReconciliationRecord> result =
                reconciliationService
                        .getLatestReconciliation(
                                transactionId);

        assertTrue(result.isPresent());

        assertEquals(
                100L,
                result.get().getId());

        assertEquals(
                ReconciliationStatus.MATCHED,
                result.get().getStatus());
    }

    @Test
    void shouldReturnReconciliationsByStatus() {

        ReconciliationRecord record1 =
                new ReconciliationRecord();

        record1.setId(100L);
        record1.setStatus(
                ReconciliationStatus.MISMATCH);

        ReconciliationRecord record2 =
                new ReconciliationRecord();

        record2.setId(101L);
        record2.setStatus(
                ReconciliationStatus.MISMATCH);

        when(reconciliationRecordRepository
                .findByStatusOrderByReconciledAtDesc(
                        ReconciliationStatus.MISMATCH))
                .thenReturn(
                        List.of(record1, record2));

        List<ReconciliationRecord> result =
                reconciliationService
                        .getByStatus(
                                ReconciliationStatus.MISMATCH);

        assertEquals(2, result.size());

        assertEquals(
                100L,
                result.get(0).getId());

        assertEquals(
                101L,
                result.get(1).getId());
    }
}