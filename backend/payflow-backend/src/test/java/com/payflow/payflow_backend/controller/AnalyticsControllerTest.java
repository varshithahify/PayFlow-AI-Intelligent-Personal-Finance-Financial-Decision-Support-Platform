package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.dto.analytics.AnalyticsOverviewResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentTrendResponse;
import com.payflow.payflow_backend.dto.analytics.PaymentStatusResponse;
import com.payflow.payflow_backend.dto.analytics.GatewayPerformanceResponse;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.service.AnalyticsService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    // --------------------------------------------------
    // ANALYTICS OVERVIEW
    // --------------------------------------------------

    @Test
    void shouldReturnAnalyticsOverview() throws Exception {

        AnalyticsOverviewResponse response =
                new AnalyticsOverviewResponse(
                        100,
                        80,
                        10,
                        5,
                        5,
                        80.0,
                        Map.of(
                                "INR", new BigDecimal("50000.00"),
                                "USD", new BigDecimal("1000.00")
                        )
                );

        when(analyticsService.getOverview())
                .thenReturn(response);

        mockMvc.perform(
                get("/api/analytics/overview")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalTransactions").value(100))
        .andExpect(jsonPath("$.successfulTransactions").value(80))
        .andExpect(jsonPath("$.failedTransactions").value(10))
        .andExpect(jsonPath("$.pendingTransactions").value(5))
        .andExpect(jsonPath("$.refundedTransactions").value(5))
        .andExpect(jsonPath("$.successRate").value(80.0))
        .andExpect(jsonPath("$.paymentVolumeByCurrency.INR")
                .value(50000.00));

        verify(analyticsService).getOverview();
    }


    // --------------------------------------------------
    // PAYMENT TRENDS
    // --------------------------------------------------

    @Test
    void shouldReturnPaymentTrends() throws Exception {

        List<PaymentTrendResponse> trends = List.of(
                new PaymentTrendResponse(
                        LocalDate.of(2026, 11, 1), 20
                ),
                new PaymentTrendResponse(
                        LocalDate.of(2026, 11, 2), 30
                )
        );

        when(analyticsService.getPaymentTrends())
                .thenReturn(trends);

        mockMvc.perform(
                get("/api/analytics/payment-trends")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].date").value("2026-11-01"))
        .andExpect(jsonPath("$[0].transactionCount").value(20))
        .andExpect(jsonPath("$[1].transactionCount").value(30));

        verify(analyticsService).getPaymentTrends();
    }


    @Test
    void shouldReturnEmptyPaymentTrends() throws Exception {

        when(analyticsService.getPaymentTrends())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/analytics/payment-trends")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(analyticsService).getPaymentTrends();
    }


    // --------------------------------------------------
    // PAYMENT STATUS DISTRIBUTION
    // --------------------------------------------------

    @Test
    void shouldReturnPaymentStatusDistribution()
            throws Exception {

        List<PaymentStatusResponse> statuses = List.of(
                new PaymentStatusResponse(
                        TransactionStatus.SUCCESS, 80
                ),
                new PaymentStatusResponse(
                        TransactionStatus.FAILED, 20
                )
        );

        when(analyticsService.getPaymentStatusDistribution())
                .thenReturn(statuses);

        mockMvc.perform(
                get("/api/analytics/payment-status")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].status").value("SUCCESS"))
        .andExpect(jsonPath("$[0].count").value(80))
        .andExpect(jsonPath("$[1].status").value("FAILED"))
        .andExpect(jsonPath("$[1].count").value(20));

        verify(analyticsService).getPaymentStatusDistribution();
    }


    @Test
    void shouldReturnEmptyPaymentStatusDistribution()
            throws Exception {

        when(analyticsService.getPaymentStatusDistribution())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/analytics/payment-status")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(analyticsService).getPaymentStatusDistribution();
    }


    // --------------------------------------------------
    // GATEWAY PERFORMANCE
    // --------------------------------------------------

    @Test
    void shouldReturnGatewayPerformance()
            throws Exception {

        List<GatewayPerformanceResponse> gateways = List.of(
                new GatewayPerformanceResponse(
                        "RAZORPAY",
                        100,
                        90,
                        10,
                        90.0,
                        Map.of(
                                "INR", new BigDecimal("45000.00")
                        )
                ),
                new GatewayPerformanceResponse(
                        "STRIPE",
                        50,
                        40,
                        10,
                        80.0,
                        Map.of(
                                "USD", new BigDecimal("2000.00")
                        )
                )
        );

        when(analyticsService.getGatewayPerformance())
                .thenReturn(gateways);

        mockMvc.perform(
                get("/api/analytics/gateway-performance")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].gatewayName").value("RAZORPAY"))
        .andExpect(jsonPath("$[0].totalTransactions").value(100))
        .andExpect(jsonPath("$[0].successRate").value(90.0))
        .andExpect(jsonPath(
                "$[0].successfulVolumeByCurrency.INR"
        ).value(45000.00))
        .andExpect(jsonPath("$[1].gatewayName").value("STRIPE"));

        verify(analyticsService).getGatewayPerformance();
    }


    @Test
    void shouldReturnEmptyGatewayPerformance()
            throws Exception {

        when(analyticsService.getGatewayPerformance())
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/analytics/gateway-performance")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(analyticsService).getGatewayPerformance();
    }
}