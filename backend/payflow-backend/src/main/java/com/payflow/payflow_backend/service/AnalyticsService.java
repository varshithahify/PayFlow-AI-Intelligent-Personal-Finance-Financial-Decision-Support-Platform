package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.dto.analytics.AnalyticsOverviewResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentTrendResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentStatusResponse;

import com.payflow.payflow_backend.entity.TransactionStatus;

import com.payflow.payflow_backend.dto.analytics.GatewayPerformanceResponse;

import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public AnalyticsService(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    // =====================================================
    // 1. ANALYTICS OVERVIEW
    // =====================================================

    public AnalyticsOverviewResponse getOverview() {

        Long orgId = TenantContext.requireOrgId();

        long totalTransactions =
                transactionRepository.countByOrgId(orgId);

        long successfulTransactions =
                transactionRepository.countByOrgIdAndStatus(
                        orgId, TransactionStatus.SUCCESS
                );

        long failedTransactions =
                transactionRepository.countByOrgIdAndStatus(
                        orgId, TransactionStatus.FAILED
                );

        long createdTransactions =
                transactionRepository.countByOrgIdAndStatus(
                        orgId, TransactionStatus.CREATED
                );

        long processingTransactions =
                transactionRepository.countByOrgIdAndStatus(
                        orgId, TransactionStatus.PROCESSING
                );

        long refundedTransactions =
                transactionRepository.countByOrgIdAndStatus(
                        orgId, TransactionStatus.REFUNDED
                );

        long pendingTransactions =
                createdTransactions + processingTransactions;

        // Calculate success rate using completed transactions.
        long completedTransactions =
                successfulTransactions
                        + failedTransactions
                        + refundedTransactions;

        double successRate = completedTransactions == 0
                ? 0.0
                : BigDecimal.valueOf(successfulTransactions)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(completedTransactions),
                                2,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue();

        // Calculate successful payment volume by currency.
        Map<String, BigDecimal> paymentVolumeByCurrency =
                new HashMap<>();

        for (Object[] row :
                transactionRepository.findPaymentVolumeByCurrency(
                        orgId,
                        TransactionStatus.SUCCESS
                )) {

            String currency = (String) row[0];
            BigDecimal volume = (BigDecimal) row[1];

            paymentVolumeByCurrency.put(currency, volume);
        }

        return new AnalyticsOverviewResponse(
                totalTransactions,
                successfulTransactions,
                failedTransactions,
                pendingTransactions,
                refundedTransactions,
                successRate,
                paymentVolumeByCurrency
        );
    }


    // =====================================================
    // 2. PAYMENT TRENDS - LAST 30 DAYS
    // =====================================================

    public List<PaymentTrendResponse> getPaymentTrends() {

        Long orgId = TenantContext.requireOrgId();

        LocalDate today = LocalDate.now();

        LocalDate startDate = today.minusDays(29);

        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        LocalDateTime endDateTime =
                today.plusDays(1).atStartOfDay();

        List<Object[]> results =
                transactionRepository.findDailyTransactionCounts(
                        orgId,
                        startDateTime,
                        endDateTime
                );

        List<PaymentTrendResponse> trends =
                new ArrayList<>();

        for (Object[] row : results) {

            LocalDate date =
                    ((java.sql.Date) row[0]).toLocalDate();

            long transactionCount =
                    ((Number) row[1]).longValue();

            trends.add(
                    new PaymentTrendResponse(
                            date,
                            transactionCount
                    )
            );
        }

        return trends;
    }


    // =====================================================
    // 3. PAYMENT STATUS DISTRIBUTION
    // =====================================================

    public List<PaymentStatusResponse>
    getPaymentStatusDistribution() {

        Long orgId = TenantContext.requireOrgId();

        List<PaymentStatusResponse> distribution =
                new ArrayList<>();

        // Include every transaction status, even if its count is zero.
        for (TransactionStatus status : TransactionStatus.values()) {

            long count =
                    transactionRepository.countByOrgIdAndStatus(
                            orgId,
                            status
                    );

            distribution.add(
                    new PaymentStatusResponse(
                            status,
                            count
                    )
            );
        }

        return distribution;
    }

    // =====================================================
// 4. GATEWAY PERFORMANCE ANALYTICS
// =====================================================

public List<GatewayPerformanceResponse> getGatewayPerformance() {

    Long orgId = TenantContext.requireOrgId();

    Map<String, long[]> gatewayCounts = new HashMap<>();

    // Retrieve gateway transaction counts grouped by status.
    List<Object[]> countResults =
            transactionRepository.findTransactionCountsByGatewayAndStatus(
                    orgId
            );

    for (Object[] row : countResults) {

        String gatewayName = (String) row[0];

        TransactionStatus status = (TransactionStatus) row[1];

        long count = ((Number) row[2]).longValue();

        long[] counts = gatewayCounts.computeIfAbsent(
                gatewayName,
                key -> new long[2]
        );

        if (status == TransactionStatus.SUCCESS) {
            counts[0] = count;
        } else if (status == TransactionStatus.FAILED) {
            counts[1] = count;
        }
    }

    // Retrieve successful payment volume grouped by gateway and currency.
    Map<String, Map<String, BigDecimal>> volumeByGateway =
            new HashMap<>();

    List<Object[]> volumeResults =
            transactionRepository.findSuccessfulPaymentVolumeByGatewayAndCurrency(
                    orgId,
                    TransactionStatus.SUCCESS
            );

    for (Object[] row : volumeResults) {

        String gatewayName = (String) row[0];

        String currency = (String) row[1];

        BigDecimal volume = (BigDecimal) row[2];

        volumeByGateway
                .computeIfAbsent(
                        gatewayName,
                        key -> new HashMap<>()
                )
                .put(currency, volume);
    }

    // Build gateway performance responses.
    List<GatewayPerformanceResponse> responses =
            new ArrayList<>();

    for (Map.Entry<String, long[]> entry : gatewayCounts.entrySet()) {

        String gatewayName = entry.getKey();

        long successful = entry.getValue()[0];

        long failed = entry.getValue()[1];

        long total = successful + failed;

        double successRate = total == 0
                ? 0.0
                : BigDecimal.valueOf(successful)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(total),
                                2,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue();

        Map<String, BigDecimal> successfulVolume =
                volumeByGateway.getOrDefault(
                        gatewayName,
                        new HashMap<>()
                );

        responses.add(
                new GatewayPerformanceResponse(
                        gatewayName,
                        total,
                        successful,
                        failed,
                        successRate,
                        successfulVolume
                )
        );
    }

    return responses;
}
}