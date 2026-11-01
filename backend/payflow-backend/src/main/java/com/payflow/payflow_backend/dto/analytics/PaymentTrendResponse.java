package com.payflow.payflow_backend.dto.analytics;

import java.time.LocalDate;

public record PaymentTrendResponse(
        LocalDate date,
        long transactionCount
) {
}