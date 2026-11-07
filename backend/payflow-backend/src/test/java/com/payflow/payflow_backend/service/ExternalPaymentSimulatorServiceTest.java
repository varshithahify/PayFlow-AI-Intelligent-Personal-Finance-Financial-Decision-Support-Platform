package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.repository.ExternalPaymentRecordRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentSimulatorServiceTest {

    @Mock
    private ExternalPaymentRecordRepository externalPaymentRecordRepository;

    @InjectMocks
    private ExternalPaymentSimulatorService simulatorService;

    @Test
    void shouldCreateExternalRecordSuccessfully() {

        Transaction transaction = mock(Transaction.class);

        when(transaction.getId()).thenReturn(100L);
        when(transaction.getOrgId()).thenReturn(10L);
        when(transaction.getAmount())
                .thenReturn(new BigDecimal("1000.00"));
        when(transaction.getCurrency()).thenReturn("INR");
        when(transaction.getStatus())
                .thenReturn(TransactionStatus.SUCCESS);

        ExternalPaymentRecord savedRecord =
                mock(ExternalPaymentRecord.class);

        when(externalPaymentRecordRepository.save(
                any(ExternalPaymentRecord.class)
        )).thenReturn(savedRecord);

        ExternalPaymentRecord result =
                simulatorService.createExternalRecord(
                        transaction,
                        "RAZORPAY"
                );

        assertNotNull(result);
        assertSame(savedRecord, result);

        verify(externalPaymentRecordRepository)
                .save(any(ExternalPaymentRecord.class));
    }

    @Test
    void shouldGenerateExternalTransactionIdWithCorrectPrefix() {

        Transaction transaction = createTransaction();

        ArgumentCaptor<ExternalPaymentRecord> captor =
                ArgumentCaptor.forClass(ExternalPaymentRecord.class);

        when(externalPaymentRecordRepository.save(any(
                ExternalPaymentRecord.class
        ))).thenAnswer(invocation -> invocation.getArgument(0));

        simulatorService.createExternalRecord(
                transaction,
                "RAZORPAY"
        );

        verify(externalPaymentRecordRepository)
                .save(captor.capture());

        ExternalPaymentRecord record = captor.getValue();

        assertNotNull(record.getExternalTransactionId());
        assertTrue(
                record.getExternalTransactionId().startsWith("EXT-")
        );
    }

    @Test
    void shouldCopyTransactionDetailsIntoExternalRecord() {

        Transaction transaction = createTransaction();

        ArgumentCaptor<ExternalPaymentRecord> captor =
                ArgumentCaptor.forClass(ExternalPaymentRecord.class);

        when(externalPaymentRecordRepository.save(any(
                ExternalPaymentRecord.class
        ))).thenAnswer(invocation -> invocation.getArgument(0));

        simulatorService.createExternalRecord(
                transaction,
                "RAZORPAY"
        );

        verify(externalPaymentRecordRepository)
                .save(captor.capture());

        ExternalPaymentRecord record = captor.getValue();

        assertEquals(100L, record.getInternalTransactionId());
        assertEquals(10L, record.getOrgId());
        assertEquals(
                new BigDecimal("1000.00"),
                record.getAmount()
        );
        assertEquals("INR", record.getCurrency());
        assertEquals("SUCCESS", record.getStatus());
        assertEquals("RAZORPAY", record.getGateway());
    }

    @Test
    void shouldGenerateUniqueExternalTransactionIds() {

        Transaction transaction = createTransaction();

        when(externalPaymentRecordRepository.save(any(
                ExternalPaymentRecord.class
        ))).thenAnswer(invocation -> invocation.getArgument(0));

        ExternalPaymentRecord first =
                simulatorService.createExternalRecord(
                        transaction,
                        "RAZORPAY"
                );

        ExternalPaymentRecord second =
                simulatorService.createExternalRecord(
                        transaction,
                        "RAZORPAY"
                );

        assertNotEquals(
                first.getExternalTransactionId(),
                second.getExternalTransactionId()
        );

        verify(externalPaymentRecordRepository, times(2))
                .save(any(ExternalPaymentRecord.class));
    }

    private Transaction createTransaction() {

        Transaction transaction = mock(Transaction.class);

        when(transaction.getId()).thenReturn(100L);
        when(transaction.getOrgId()).thenReturn(10L);
        when(transaction.getAmount())
                .thenReturn(new BigDecimal("1000.00"));
        when(transaction.getCurrency()).thenReturn("INR");
        when(transaction.getStatus())
                .thenReturn(TransactionStatus.SUCCESS);

        return transaction;
    }
}