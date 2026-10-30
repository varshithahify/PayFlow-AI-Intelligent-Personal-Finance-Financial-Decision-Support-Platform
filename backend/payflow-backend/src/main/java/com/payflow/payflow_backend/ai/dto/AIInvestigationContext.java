package com.payflow.payflow_backend.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AIInvestigationContext(
        Long transactionId,
        BigDecimal transactionAmount,
        String transactionCurrency,
        String paymentMethod,
        String transactionStatus,
        String gatewayName,
        LocalDateTime transactionCreatedAt,

        Long reconciliationRecordId,
        String reconciliationStatus,
        BigDecimal internalAmount,
        BigDecimal externalAmount,
        String internalStatus,
        String externalStatus,
        String mismatchReason,
        LocalDateTime reconciledAt,

        Long investigationId,
        String issueType,
        String investigationPriority,
        String investigationStatus,
        String existingSummary
) {
}