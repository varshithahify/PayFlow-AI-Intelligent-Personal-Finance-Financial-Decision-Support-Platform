package com.payflow.payflow_backend.dto.analytics;

import com.payflow.payflow_backend.entity.TransactionStatus;

public record PaymentStatusResponse(
        TransactionStatus status,
        long count
) {
}