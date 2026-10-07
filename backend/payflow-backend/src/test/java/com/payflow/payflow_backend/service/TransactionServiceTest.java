package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.event.TransactionEvent;
import com.payflow.payflow_backend.event.TransactionEventProducer;
import com.payflow.payflow_backend.gateway.GatewayHealthService;
import com.payflow.payflow_backend.gateway.GatewayResult;
import com.payflow.payflow_backend.gateway.GatewayRouter;
import com.payflow.payflow_backend.gateway.PaymentGateway;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GatewayRouter gatewayRouter;

    @Mock
    private GatewayHealthService gatewayHealthService;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @Mock
    private TransactionCacheService transactionCacheService;

    @Mock
    private PaymentGateway gatewayA;

    @Mock
    private PaymentGateway gatewayB;

    @Mock
    private User user;

    private TransactionService transactionService;

    private static final Long USER_ID = 1L;
    private static final Long TRANSACTION_ID = 100L;
    private static final String EMAIL = "merchant@test.com";

    @BeforeEach
    void setUp() {

        when(user.getId())
                .thenReturn(USER_ID);

        transactionService = new TransactionService(
                transactionRepository,
                userRepository,
                gatewayRouter,
                gatewayHealthService,
                transactionEventProducer,
                transactionCacheService
        );
    }

    @Test
    void shouldReturnExistingTransactionForSameIdempotencyKey() {

        Transaction existing = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-123"
        );

        TransactionRequest request = createRequest();

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(transactionRepository.findByUserIdAndIdempotencyKey(
                USER_ID,
                "idem-123"
        )).thenReturn(Optional.of(existing));

        TransactionResponse response =
                transactionService.createTransaction(
                        request,
                        EMAIL,
                        "idem-123"
                );

        assertNotNull(response);

        verify(transactionRepository, never())
                .save(any(Transaction.class));

        verify(transactionEventProducer, never())
                .publish(any(TransactionEvent.class));
    }

    @Test
    void shouldRejectSameIdempotencyKeyForDifferentRequest() {

        Transaction existing = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-123"
        );

        TransactionRequest differentRequest =
                createRequest();

        differentRequest.setAmount(
                new BigDecimal("999.00")
        );

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(transactionRepository.findByUserIdAndIdempotencyKey(
                USER_ID,
                "idem-123"
        )).thenReturn(Optional.of(existing));

        assertThrows(
                IllegalStateException.class,
                () -> transactionService.createTransaction(
                        differentRequest,
                        EMAIL,
                        "idem-123"
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));

        verify(transactionEventProducer, never())
                .publish(any(TransactionEvent.class));
    }

    @Test
    void shouldProcessTransactionSuccessfully() {

        Transaction transaction = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-success"
        );

        when(transactionRepository.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(gatewayRouter.routeAll(transaction))
                .thenReturn(List.of(gatewayA));

        when(gatewayA.processPayment(transaction))
                .thenReturn(
                        GatewayResult.success(
                                "GatewayA",
                                "Payment successful"
                        )
                );

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        TransactionResponse response =
                transactionService.startProcessing(
                        TRANSACTION_ID,
                        EMAIL
                );

        assertNotNull(response);

        assertEquals(
                TransactionStatus.SUCCESS,
                transaction.getStatus()
        );

        verify(gatewayA, times(1))
                .processPayment(transaction);

        verify(gatewayHealthService, times(1))
                .recordResult(
                        eq("GatewayA"),
                        eq(true),
                        anyInt()
                );

        verify(transactionRepository, times(2))
                .save(transaction);
    }

    @Test
    void shouldUseFallbackGatewayWhenFirstGatewayFails() {

        Transaction transaction = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-fallback"
        );

        when(transactionRepository.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(gatewayRouter.routeAll(transaction))
                .thenReturn(List.of(gatewayA, gatewayB));

        when(gatewayA.processPayment(transaction))
                .thenReturn(
                        GatewayResult.failure(
                                "GatewayA",
                                "Gateway unavailable"
                        )
                );

        when(gatewayB.processPayment(transaction))
                .thenReturn(
                        GatewayResult.success(
                                "GatewayB",
                                "Payment successful"
                        )
                );

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        TransactionResponse response =
                transactionService.startProcessing(
                        TRANSACTION_ID,
                        EMAIL
                );

        assertNotNull(response);

        assertEquals(
                TransactionStatus.SUCCESS,
                transaction.getStatus()
        );

        verify(gatewayA, times(1))
                .processPayment(transaction);

        verify(gatewayB, times(1))
                .processPayment(transaction);

        verify(gatewayHealthService)
                .recordResult(
                        eq("GatewayA"),
                        eq(false),
                        anyInt()
                );

        verify(gatewayHealthService)
                .recordResult(
                        eq("GatewayB"),
                        eq(true),
                        anyInt()
                );
    }

    @Test
    void shouldMarkTransactionFailedWhenAllGatewaysFail() {

        Transaction transaction = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-failed"
        );

        when(transactionRepository.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(gatewayRouter.routeAll(transaction))
                .thenReturn(List.of(gatewayA, gatewayB));

        when(gatewayA.processPayment(transaction))
                .thenReturn(
                        GatewayResult.failure(
                                "GatewayA",
                                "Gateway A failed"
                        )
                );

        when(gatewayB.processPayment(transaction))
                .thenReturn(
                        GatewayResult.failure(
                                "GatewayB",
                                "Gateway B failed"
                        )
                );

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        TransactionResponse response =
                transactionService.startProcessing(
                        TRANSACTION_ID,
                        EMAIL
                );

        assertNotNull(response);

        assertEquals(
                TransactionStatus.FAILED,
                transaction.getStatus()
        );

        verify(gatewayA, times(1))
                .processPayment(transaction);

        verify(gatewayB, times(1))
                .processPayment(transaction);

        verify(gatewayHealthService)
                .recordResult(
                        eq("GatewayA"),
                        eq(false),
                        anyInt()
                );

        verify(gatewayHealthService)
                .recordResult(
                        eq("GatewayB"),
                        eq(false),
                        anyInt()
                );
    }

    @Test
    void shouldRecordHealthForEveryGatewayAttempt() {

        Transaction transaction = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-health"
        );

        when(transactionRepository.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(gatewayRouter.routeAll(transaction))
                .thenReturn(List.of(gatewayA, gatewayB));

        when(gatewayA.processPayment(transaction))
                .thenReturn(
                        GatewayResult.failure(
                                "GatewayA",
                                "Failed"
                        )
                );

        when(gatewayB.processPayment(transaction))
                .thenReturn(
                        GatewayResult.success(
                                "GatewayB",
                                "Success"
                        )
                );

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        transactionService.startProcessing(
                TRANSACTION_ID,
                EMAIL
        );

        verify(gatewayHealthService, times(2))
                .recordResult(
                        anyString(),
                        anyBoolean(),
                        anyInt()
                );
    }

    @Test
    void shouldPublishKafkaEventsDuringProcessing() {

        Transaction transaction = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-kafka"
        );

        when(transactionRepository.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(gatewayRouter.routeAll(transaction))
                .thenReturn(List.of(gatewayA));

        when(gatewayA.processPayment(transaction))
                .thenReturn(
                        GatewayResult.success(
                                "GatewayA",
                                "Payment successful"
                        )
                );

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        transactionService.startProcessing(
                TRANSACTION_ID,
                EMAIL
        );

        verify(transactionEventProducer, times(2))
                .publish(any(TransactionEvent.class));
    }

    @Test
    void shouldEvictCacheWhenTransactionStateChanges() {

        Transaction transaction = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-cache"
        );

        when(transactionRepository.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(gatewayRouter.routeAll(transaction))
                .thenReturn(List.of(gatewayA));

        when(gatewayA.processPayment(transaction))
                .thenReturn(
                        GatewayResult.success(
                                "GatewayA",
                                "Payment successful"
                        )
                );

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        transactionService.startProcessing(
                TRANSACTION_ID,
                EMAIL
        );

        verify(transactionCacheService, times(2))
                .evictTransaction(
                        USER_ID,
                        TRANSACTION_ID
                );
    }

    private Transaction createTransaction(
            Long id,
            TransactionStatus status,
            String idempotencyKey) {

        Transaction transaction = new Transaction(
                new BigDecimal("100.00"),
                "INR",
                "UPI",
                status,
                USER_ID,
                idempotencyKey
        );

        transaction.setId(id);

        return transaction;
    }

    private TransactionRequest createRequest() {

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(
                new BigDecimal("100.00")
        );

        request.setCurrency("INR");

        request.setPaymentMethod("UPI");

        return request;
    }
}