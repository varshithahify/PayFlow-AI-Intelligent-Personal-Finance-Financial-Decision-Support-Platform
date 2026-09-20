package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;
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

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void shouldReturnMatchedWhenInternalAndExternalRecordsMatch() {

        Transaction transaction = createTransaction(
                7L,
                "9999.00",
                "INR",
                TransactionStatus.SUCCESS);

        ExternalPaymentRecord externalRecord =
                createExternalRecord(
                        1L,
                        7L,
                        "9999.00",
                        "INR",
                        "SUCCESS");

        when(transactionRepository.findById(7L))
                .thenReturn(Optional.of(transaction));

        when(externalPaymentRecordRepository
                .findByInternalTransactionId(7L))
                .thenReturn(Optional.of(externalRecord));

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationRecord result =
                reconciliationService.reconcileTransaction(7L);

        assertEquals(
                ReconciliationStatus.MATCHED,
                result.getStatus());

        assertEquals(
                new BigDecimal("9999.00"),
                result.getInternalAmount());

        assertEquals(
                new BigDecimal("9999.00"),
                result.getExternalAmount());

        assertEquals(
                "SUCCESS",
                result.getInternalStatus());

        assertEquals(
                "SUCCESS",
                result.getExternalStatus());

        assertNull(result.getMismatchReason());

        verify(reconciliationRecordRepository)
                .save(any(ReconciliationRecord.class));
    }

    @Test
    void shouldReturnMismatchWhenAmountDoesNotMatch() {

        Transaction transaction = createTransaction(
                7L,
                "9999.00",
                "INR",
                TransactionStatus.SUCCESS);

        ExternalPaymentRecord externalRecord =
                createExternalRecord(
                        1L,
                        7L,
                        "8999.00",
                        "INR",
                        "SUCCESS");

        when(transactionRepository.findById(7L))
                .thenReturn(Optional.of(transaction));

        when(externalPaymentRecordRepository
                .findByInternalTransactionId(7L))
                .thenReturn(Optional.of(externalRecord));

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationRecord result =
                reconciliationService.reconcileTransaction(7L);

        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.getStatus());

        assertEquals(
                new BigDecimal("9999.00"),
                result.getInternalAmount());

        assertEquals(
                new BigDecimal("8999.00"),
                result.getExternalAmount());

        assertEquals(
                "Amount mismatch.",
                result.getMismatchReason());

        verify(reconciliationRecordRepository)
                .save(any(ReconciliationRecord.class));
    }

    @Test
    void shouldReturnMissingExternalWhenExternalRecordDoesNotExist() {

        Transaction transaction = createTransaction(
                6L,
                "2500.00",
                "INR",
                TransactionStatus.CREATED);

        when(transactionRepository.findById(6L))
                .thenReturn(Optional.of(transaction));

        when(externalPaymentRecordRepository
                .findByInternalTransactionId(6L))
                .thenReturn(Optional.empty());

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationRecord result =
                reconciliationService.reconcileTransaction(6L);

        assertEquals(
                ReconciliationStatus.MISSING_EXTERNAL,
                result.getStatus());

        assertEquals(
                6L,
                result.getTransactionId());

        assertNull(result.getExternalRecordId());

        assertEquals(
                new BigDecimal("2500.00"),
                result.getInternalAmount());

        assertNull(result.getExternalAmount());

        assertEquals(
                "CREATED",
                result.getInternalStatus());

        assertNull(result.getExternalStatus());

        assertEquals(
                "No matching external payment record found",
                result.getMismatchReason());

        verify(reconciliationRecordRepository)
                .save(any(ReconciliationRecord.class));
    }

    @Test
    void shouldReturnMissingInternalWhenExternalRecordHasNoInternalTransaction() {

        ExternalPaymentRecord externalRecord =
                createExternalRecord(
                        2L,
                        null,
                        "1500.00",
                        "INR",
                        "SUCCESS");

        when(externalPaymentRecordRepository.findById(2L))
                .thenReturn(Optional.of(externalRecord));

        when(reconciliationRecordRepository.save(
                any(ReconciliationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationRecord result =
                reconciliationService.reconcileExternalRecord(2L);

        assertEquals(
                ReconciliationStatus.MISSING_INTERNAL,
                result.getStatus());

        assertNull(result.getTransactionId());

        assertEquals(
                2L,
                result.getExternalRecordId());

        assertNull(result.getInternalAmount());

        assertEquals(
                new BigDecimal("1500.00"),
                result.getExternalAmount());

        assertNull(result.getInternalStatus());

        assertEquals(
                "SUCCESS",
                result.getExternalStatus());

        assertEquals(
                "No matching internal transaction found",
                result.getMismatchReason());

        verify(reconciliationRecordRepository)
                .save(any(ReconciliationRecord.class));
    }

    @Test
    void shouldReturnLatestReconciliationForTransaction() {

        ReconciliationRecord record =
                new ReconciliationRecord();

        record.setId(2L);
        record.setTransactionId(7L);
        record.setStatus(ReconciliationStatus.MISMATCH);
        record.setReconciledAt(LocalDateTime.now());

        when(reconciliationRecordRepository
                .findTopByTransactionIdOrderByReconciledAtDesc(7L))
                .thenReturn(Optional.of(record));

        Optional<ReconciliationRecord> result =
                reconciliationService
                        .getLatestReconciliation(7L);

        assertTrue(result.isPresent());

        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.get().getStatus());

        assertEquals(
                7L,
                result.get().getTransactionId());

        verify(reconciliationRecordRepository)
                .findTopByTransactionIdOrderByReconciledAtDesc(7L);
    }

    @Test
    void shouldReturnReconciliationRecordsByStatus() {

        ReconciliationRecord record =
                new ReconciliationRecord();

        record.setId(2L);
        record.setTransactionId(7L);
        record.setStatus(ReconciliationStatus.MISMATCH);
        record.setMismatchReason("Amount mismatch.");

        when(reconciliationRecordRepository
                .findByStatusOrderByReconciledAtDesc(
                        ReconciliationStatus.MISMATCH))
                .thenReturn(List.of(record));

        List<ReconciliationRecord> result =
                reconciliationService.getByStatus(
                        ReconciliationStatus.MISMATCH);

        assertEquals(1, result.size());

        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.get(0).getStatus());

        assertEquals(
                "Amount mismatch.",
                result.get(0).getMismatchReason());

        verify(reconciliationRecordRepository)
                .findByStatusOrderByReconciledAtDesc(
                        ReconciliationStatus.MISMATCH);
    }

    private Transaction createTransaction(
            Long id,
            String amount,
            String currency,
            TransactionStatus status) {

        Transaction transaction =
                new Transaction();

        transaction.setId(id);
        transaction.setAmount(
                new BigDecimal(amount));
        transaction.setCurrency(currency);
        transaction.setStatus(status);

        return transaction;
    }

    private ExternalPaymentRecord createExternalRecord(
            Long id,
            Long internalTransactionId,
            String amount,
            String currency,
            String status) {

        ExternalPaymentRecord record =
                new ExternalPaymentRecord();

        record.setId(id);
        record.setInternalTransactionId(
                internalTransactionId);
        record.setAmount(
                new BigDecimal(amount));
        record.setCurrency(currency);
        record.setStatus(status);

        return record;
    }
}