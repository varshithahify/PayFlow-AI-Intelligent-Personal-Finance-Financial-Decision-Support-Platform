package com.payflow.payflow_backend.dto.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsOverviewResponse(

        long totalTransactions,

        long successfulTransactions,

        long failedTransactions,

        long pendingTransactions,

        long refundedTransactions,

        double successRate,

        Map<String, BigDecimal> paymentVolumeByCurrency

) {
}