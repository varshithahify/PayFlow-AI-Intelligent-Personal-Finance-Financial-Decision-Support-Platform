package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.analytics.AnalyticsOverviewResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentTrendResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentStatusResponse;
import com.payflow.payflow_backend.dto.analytics.GatewayPerformanceResponse;

import com.payflow.payflow_backend.service.AnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // 1. Analytics overview
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {

        AnalyticsOverviewResponse response =
                analyticsService.getOverview();

        return ResponseEntity.ok(response);
    }

    // 2. Payment trends
    @GetMapping("/payment-trends")
    public ResponseEntity<List<PaymentTrendResponse>> getPaymentTrends() {

        List<PaymentTrendResponse> response =
                analyticsService.getPaymentTrends();

        return ResponseEntity.ok(response);
    }

    // 3. Payment status distribution
    @GetMapping("/payment-status")
    public ResponseEntity<List<PaymentStatusResponse>>
    getPaymentStatusDistribution() {

        List<PaymentStatusResponse> response =
                analyticsService.getPaymentStatusDistribution();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/gateway-performance")
public ResponseEntity<List<GatewayPerformanceResponse>> getGatewayPerformance() {

    List<GatewayPerformanceResponse> response =
            analyticsService.getGatewayPerformance();

    return ResponseEntity.ok(response);
}
}