package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;
import com.payflow.payflow_backend.event.TransactionEvent;
import com.payflow.payflow_backend.event.TransactionEventProducer;
import com.payflow.payflow_backend.event.TransactionEventType;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.gateway.GatewayHealthService;
import com.payflow.payflow_backend.gateway.GatewayResult;
import com.payflow.payflow_backend.gateway.GatewayRouter;
import com.payflow.payflow_backend.gateway.PaymentGateway;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private static final Long ORG_ID = 1L;
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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

        Transaction existing = createTransaction(
                TRANSACTION_ID,
                TransactionStatus.CREATED,
                "idem-123"
        );

        TransactionRequest request = createRequest();

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(user);

        when(transactionRepository.findByOrgIdAndUserIdAndIdempotencyKey(
                ORG_ID,
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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

        when(transactionRepository.findByOrgIdAndUserIdAndIdempotencyKey(
                ORG_ID,
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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

        when(user.getOrgId())
                .thenReturn(ORG_ID);

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

    @Test
void shouldRefundSuccessfulTransactionUsingOriginalGateway() {

    when(user.getOrgId())
            .thenReturn(ORG_ID);

    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.SUCCESS,
            "idem-refund");

    transaction.setGatewayName("GatewayA");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));

    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);

    when(gatewayRouter.getGatewayByName("GatewayA"))
            .thenReturn(gatewayA);

    when(gatewayA.refundPayment(transaction))
            .thenReturn(
                    GatewayResult.success(
                            "GatewayA",
                            "Refund successful"));

    when(transactionRepository.save(transaction))
            .thenReturn(transaction);

    TransactionResponse response =
            transactionService.refundTransaction(
                    TRANSACTION_ID,
                    EMAIL);

    assertNotNull(response);

    assertEquals(
            TransactionStatus.REFUNDED,
            transaction.getStatus());

    assertEquals(
            "GatewayA",
            transaction.getGatewayName());

    verify(gatewayRouter, times(1))
            .getGatewayByName("GatewayA");

    verify(gatewayA, times(1))
            .refundPayment(transaction);

    verify(transactionRepository, times(1))
            .save(transaction);

    verify(transactionCacheService, times(1))
            .evictTransaction(
                    USER_ID,
                    TRANSACTION_ID);

    verify(transactionEventProducer, times(1))
            .publish(any(TransactionEvent.class));
}

        @Test
void shouldRejectRefundForNonSuccessfulTransaction() {

    when(user.getOrgId())
            .thenReturn(ORG_ID);

    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.CREATED,
            "idem-refund-invalid");

    transaction.setGatewayName("GatewayA");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));

    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);

    assertThrows(
            IllegalStateException.class,
            () -> transactionService.refundTransaction(
                    TRANSACTION_ID,
                    EMAIL));

    verify(gatewayRouter, never())
            .getGatewayByName(anyString());

    verify(gatewayA, never())
            .refundPayment(any(Transaction.class));

    verify(transactionRepository, never())
            .save(any(Transaction.class));

    verify(transactionEventProducer, never())
            .publish(any(TransactionEvent.class));
}

        @Test
void shouldRejectRefundWhenOriginalGatewayIsMissing() {

    when(user.getOrgId())
            .thenReturn(ORG_ID);

    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.SUCCESS,
            "idem-refund-no-gateway");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));

    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);

    assertThrows(
            IllegalStateException.class,
            () -> transactionService.refundTransaction(
                    TRANSACTION_ID,
                    EMAIL));

    verify(gatewayRouter, never())
            .getGatewayByName(anyString());

    verify(gatewayA, never())
            .refundPayment(any(Transaction.class));

    verify(gatewayB, never())
            .refundPayment(any(Transaction.class));

    verify(transactionRepository, never())
            .save(any(Transaction.class));

    verify(transactionEventProducer, never())
            .publish(any(TransactionEvent.class));
}
        @Test
void shouldRejectRefundWhenGatewayRefundFails() {
    when(user.getOrgId()).thenReturn(ORG_ID);

    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.SUCCESS,
            "idem-refund-gateway-failure");
    transaction.setGatewayName("GatewayA");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));
    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);
    when(gatewayRouter.getGatewayByName("GatewayA"))
            .thenReturn(gatewayA);
    when(gatewayA.refundPayment(transaction))
            .thenReturn(GatewayResult.failure(
                    "GatewayA",
                    "Refund declined by gateway"));

    assertThrows(
            IllegalStateException.class,
            () -> transactionService.refundTransaction(
                    TRANSACTION_ID,
                    EMAIL));

    assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());

    verify(gatewayRouter).getGatewayByName("GatewayA");
    verify(gatewayA).refundPayment(transaction);
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(transactionCacheService, never())
            .evictTransaction(anyLong(), anyLong());
    verify(transactionEventProducer, never())
            .publish(any(TransactionEvent.class));
}

        @Test
void shouldRejectRefundForAlreadyRefundedTransaction() {
    when(user.getOrgId()).thenReturn(ORG_ID);

    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.REFUNDED,
            "idem-refund-already-refunded");
    transaction.setGatewayName("GatewayA");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));
    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);

    assertThrows(
            IllegalStateException.class,
            () -> transactionService.refundTransaction(
                    TRANSACTION_ID,
                    EMAIL));

    verify(gatewayRouter, never()).getGatewayByName(anyString());
    verify(gatewayA, never()).refundPayment(any(Transaction.class));
    verify(gatewayB, never()).refundPayment(any(Transaction.class));
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(transactionEventProducer, never())
            .publish(any(TransactionEvent.class));
}

        @Test
void shouldRejectRefundWhenTransactionBelongsToAnotherOrganization() {
    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.SUCCESS,
            "idem-refund-other-org");

    transaction.setUserId(USER_ID + 1);
    transaction.setOrgId(ORG_ID + 1);
    transaction.setGatewayName("GatewayA");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));

    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);

    assertThrows(
            ResourceNotFoundException.class,
            () -> transactionService.refundTransaction(
                    TRANSACTION_ID,
                    EMAIL));

    verify(gatewayRouter, never()).getGatewayByName(anyString());
    verify(gatewayA, never()).refundPayment(any(Transaction.class));
    verify(gatewayB, never()).refundPayment(any(Transaction.class));
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(transactionEventProducer, never())
            .publish(any(TransactionEvent.class));
}
        @Test
void shouldPublishRefundedEventAfterSuccessfulRefund() {
    when(user.getOrgId()).thenReturn(ORG_ID);

    Transaction transaction = createTransaction(
            TRANSACTION_ID,
            TransactionStatus.SUCCESS,
            "idem-refund-event");
    transaction.setGatewayName("GatewayA");

    when(transactionRepository.findById(TRANSACTION_ID))
            .thenReturn(Optional.of(transaction));
    when(userRepository.findByEmail(EMAIL))
            .thenReturn(user);
    when(gatewayRouter.getGatewayByName("GatewayA"))
            .thenReturn(gatewayA);
    when(gatewayA.refundPayment(transaction))
            .thenReturn(GatewayResult.success(
                    "GatewayA",
                    "Refund successful"));
    when(transactionRepository.save(transaction))
            .thenReturn(transaction);

    transactionService.refundTransaction(
            TRANSACTION_ID,
            EMAIL);

    ArgumentCaptor<TransactionEvent> eventCaptor =
            ArgumentCaptor.forClass(TransactionEvent.class);

    verify(transactionEventProducer).publish(eventCaptor.capture());

    TransactionEvent event = eventCaptor.getValue();

    assertEquals(
            TransactionEventType.REFUNDED,
            event.getEventType());

    assertEquals(
            TransactionStatus.REFUNDED,
            event.getStatus());

    assertEquals(
            TRANSACTION_ID,
            event.getTransactionId());

    assertEquals(
            ORG_ID,
            event.getOrgId());
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
                ORG_ID,
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