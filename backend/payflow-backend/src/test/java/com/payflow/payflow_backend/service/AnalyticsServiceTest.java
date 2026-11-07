package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.analytics.AnalyticsOverviewResponse;
import com.payflow.payflow_backend.dto.analytics.GatewayPerformanceResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentStatusResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentTrendResponse;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Long ORG_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(ORG_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==========================================
    // 1. ANALYTICS OVERVIEW
    // ==========================================

    @Test
    void shouldReturnAnalyticsOverview() {

        when(transactionRepository.countByOrgId(ORG_ID))
                .thenReturn(10L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.SUCCESS))
                .thenReturn(6L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.FAILED))
                .thenReturn(2L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.CREATED))
                .thenReturn(1L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.PROCESSING))
                .thenReturn(1L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.REFUNDED))
                .thenReturn(1L);

        when(transactionRepository.findPaymentVolumeByCurrency(
                ORG_ID, TransactionStatus.SUCCESS))
                .thenReturn(List.<Object[]>of(
                        new Object[]{
                                "USD",
                                new BigDecimal("1000.00")
                        },
                        new Object[]{
                                "INR",
                                new BigDecimal("5000.00")
                        }
                ));

        AnalyticsOverviewResponse response =
                analyticsService.getOverview();

        assertAll(
                () -> assertEquals(10L, response.totalTransactions()),
                () -> assertEquals(6L, response.successfulTransactions()),
                () -> assertEquals(2L, response.failedTransactions()),
                () -> assertEquals(2L, response.pendingTransactions()),
                () -> assertEquals(1L, response.refundedTransactions()),
                () -> assertEquals(66.67, response.successRate(), 0.001),
                () -> assertEquals(
                        new BigDecimal("1000.00"),
                        response.paymentVolumeByCurrency().get("USD")
                ),
                () -> assertEquals(
                        new BigDecimal("5000.00"),
                        response.paymentVolumeByCurrency().get("INR")
                )
        );

        verify(transactionRepository).countByOrgId(ORG_ID);
    }

    @Test
    void shouldReturnZeroSuccessRateWhenNoTransactionsAreCompleted() {

        when(transactionRepository.countByOrgId(ORG_ID))
                .thenReturn(3L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.SUCCESS))
                .thenReturn(0L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.FAILED))
                .thenReturn(0L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.CREATED))
                .thenReturn(2L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.PROCESSING))
                .thenReturn(1L);

        when(transactionRepository.countByOrgIdAndStatus(
                ORG_ID, TransactionStatus.REFUNDED))
                .thenReturn(0L);

        when(transactionRepository.findPaymentVolumeByCurrency(
                ORG_ID, TransactionStatus.SUCCESS))
                .thenReturn(List.of());

        AnalyticsOverviewResponse response =
                analyticsService.getOverview();

        assertEquals(0.0, response.successRate());
        assertEquals(3L, response.pendingTransactions());
        assertTrue(response.paymentVolumeByCurrency().isEmpty());
    }

    // ==========================================
    // 2. PAYMENT TRENDS
    // ==========================================

    @Test
    void shouldReturnPaymentTrends() {

        LocalDate firstDate = LocalDate.of(2026, 11, 1);
        LocalDate secondDate = LocalDate.of(2026, 11, 2);

        when(transactionRepository.findDailyTransactionCounts(
                eq(ORG_ID),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.<Object[]>of(
                new Object[]{
                        Date.valueOf(firstDate),
                        5L
                },
                new Object[]{
                        Date.valueOf(secondDate),
                        8L
                }
        ));

        List<PaymentTrendResponse> response =
                analyticsService.getPaymentTrends();

        assertEquals(2, response.size());

        assertEquals(firstDate, response.get(0).date());
        assertEquals(5L, response.get(0).transactionCount());

        assertEquals(secondDate, response.get(1).date());
        assertEquals(8L, response.get(1).transactionCount());

        verify(transactionRepository).findDailyTransactionCounts(
                eq(ORG_ID),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoPaymentTrendsExist() {

        when(transactionRepository.findDailyTransactionCounts(
                eq(ORG_ID),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        List<PaymentTrendResponse> response =
                analyticsService.getPaymentTrends();

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    // ==========================================
    // 3. PAYMENT STATUS DISTRIBUTION
    // ==========================================

    @Test
    void shouldReturnDistributionForEveryTransactionStatus() {

        when(transactionRepository.countByOrgIdAndStatus(
                eq(ORG_ID),
                any(TransactionStatus.class)
        )).thenAnswer(invocation -> {

            TransactionStatus status = invocation.getArgument(1);

            return switch (status) {
                case SUCCESS -> 10L;
                case FAILED -> 3L;
                case CREATED -> 2L;
                case PROCESSING -> 4L;
                case REFUNDED -> 1L;
            };
        });

        List<PaymentStatusResponse> response =
                analyticsService.getPaymentStatusDistribution();

        assertEquals(TransactionStatus.values().length, response.size());

        assertEquals(
                10L,
                response.stream()
                        .filter(item -> item.status() == TransactionStatus.SUCCESS)
                        .findFirst()
                        .orElseThrow()
                        .count()
        );

        assertEquals(
                3L,
                response.stream()
                        .filter(item -> item.status() == TransactionStatus.FAILED)
                        .findFirst()
                        .orElseThrow()
                        .count()
        );

        assertEquals(
                TransactionStatus.values().length,
                response.size()
        );

        verify(transactionRepository, times(
                TransactionStatus.values().length
        )).countByOrgIdAndStatus(
                eq(ORG_ID),
                any(TransactionStatus.class)
        );
    }

    // ==========================================
    // 4. GATEWAY PERFORMANCE
    // ==========================================

    @Test
    void shouldReturnGatewayPerformanceAnalytics() {

        when(transactionRepository.findTransactionCountsByGatewayAndStatus(
                ORG_ID
        )).thenReturn(List.<Object[]>of(

                new Object[]{
                        "RAZORPAY",
                        TransactionStatus.SUCCESS,
                        8L
                },

                new Object[]{
                        "RAZORPAY",
                        TransactionStatus.FAILED,
                        2L
                },

                new Object[]{
                        "STRIPE",
                        TransactionStatus.SUCCESS,
                        5L
                },

                new Object[]{
                        "STRIPE",
                        TransactionStatus.FAILED,
                        5L
                }
        ));

        when(transactionRepository.findSuccessfulPaymentVolumeByGatewayAndCurrency(
                ORG_ID,
                TransactionStatus.SUCCESS
        )).thenReturn(List.<Object[]>of(

                new Object[]{
                        "RAZORPAY",
                        "INR",
                        new BigDecimal("10000.00")
                },

                new Object[]{
                        "STRIPE",
                        "USD",
                        new BigDecimal("5000.00")
                }
        ));

        List<GatewayPerformanceResponse> response =
                analyticsService.getGatewayPerformance();

        assertEquals(2, response.size());

        GatewayPerformanceResponse razorpay =
                response.stream()
                        .filter(item -> item.gatewayName().equals("RAZORPAY"))
                        .findFirst()
                        .orElseThrow();

        assertAll(
                () -> assertEquals(10L, razorpay.totalTransactions()),
                () -> assertEquals(8L, razorpay.successfulTransactions()),
                () -> assertEquals(2L, razorpay.failedTransactions()),
                () -> assertEquals(80.0, razorpay.successRate(), 0.001),
                () -> assertEquals(
                        new BigDecimal("10000.00"),
                        razorpay.successfulVolumeByCurrency().get("INR")
                )
        );

        GatewayPerformanceResponse stripe =
                response.stream()
                        .filter(item -> item.gatewayName().equals("STRIPE"))
                        .findFirst()
                        .orElseThrow();

        assertAll(
                () -> assertEquals(10L, stripe.totalTransactions()),
                () -> assertEquals(5L, stripe.successfulTransactions()),
                () -> assertEquals(5L, stripe.failedTransactions()),
                () -> assertEquals(50.0, stripe.successRate(), 0.001),
                () -> assertEquals(
                        new BigDecimal("5000.00"),
                        stripe.successfulVolumeByCurrency().get("USD")
                )
        );
    }

    @Test
    void shouldReturnZeroSuccessRateForGatewayWithoutSuccessfulPayments() {

        when(transactionRepository.findTransactionCountsByGatewayAndStatus(
                ORG_ID
        )).thenReturn(List.<Object[]>of(
                new Object[]{
                        "STRIPE",
                        TransactionStatus.FAILED,
                        5L
                }
        ));

        when(transactionRepository.findSuccessfulPaymentVolumeByGatewayAndCurrency(
                ORG_ID,
                TransactionStatus.SUCCESS
        )).thenReturn(List.of());

        List<GatewayPerformanceResponse> response =
                analyticsService.getGatewayPerformance();

        assertEquals(1, response.size());

        GatewayPerformanceResponse gateway = response.get(0);

        assertEquals(0.0, gateway.successRate());
        assertEquals(5L, gateway.failedTransactions());
        assertEquals(0L, gateway.successfulTransactions());
        assertTrue(gateway.successfulVolumeByCurrency().isEmpty());
    }

    @Test
    void shouldReturnZeroSuccessRateForGatewayWithOnlyOtherStatuses() {

        when(transactionRepository.findTransactionCountsByGatewayAndStatus(
                ORG_ID
        )).thenReturn(List.<Object[]>of(
                new Object[]{
                        "RAZORPAY",
                        TransactionStatus.CREATED,
                        4L
                }
        ));

        when(transactionRepository.findSuccessfulPaymentVolumeByGatewayAndCurrency(
                ORG_ID,
                TransactionStatus.SUCCESS
        )).thenReturn(List.of());

        List<GatewayPerformanceResponse> response =
                analyticsService.getGatewayPerformance();

        assertEquals(1, response.size());
        assertEquals(0L, response.get(0).totalTransactions());
        assertEquals(0.0, response.get(0).successRate());
    }

    @Test
    void shouldReturnEmptyGatewayPerformanceWhenNoGatewayTransactionsExist() {

        when(transactionRepository.findTransactionCountsByGatewayAndStatus(
                ORG_ID
        )).thenReturn(List.of());

        when(transactionRepository.findSuccessfulPaymentVolumeByGatewayAndCurrency(
                ORG_ID,
                TransactionStatus.SUCCESS
        )).thenReturn(List.of());

        List<GatewayPerformanceResponse> response =
                analyticsService.getGatewayPerformance();

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    // ==========================================
    // 5. TENANT CONTEXT
    // ==========================================

    @Test
    void shouldRejectAnalyticsRequestWithoutOrganizationContext() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> analyticsService.getOverview()
        );

        verifyNoInteractions(transactionRepository);
    }
}