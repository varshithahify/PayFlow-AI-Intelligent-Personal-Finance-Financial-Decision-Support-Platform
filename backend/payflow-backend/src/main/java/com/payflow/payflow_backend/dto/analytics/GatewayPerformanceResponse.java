package com.payflow.payflow_backend.dto.analytics;

import java.math.BigDecimal;
import java.util.Map;

public record GatewayPerformanceResponse(

        String gatewayName,

        long totalTransactions,

        long successfulTransactions,

        long failedTransactions,

        double successRate,

        Map<String, BigDecimal> successfulVolumeByCurrency

) {
}