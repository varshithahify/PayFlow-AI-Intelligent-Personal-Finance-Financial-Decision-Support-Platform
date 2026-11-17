package com.payflow.payflow_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateSettlementJobRequest {

    @NotEmpty(message = "Transaction IDs must not be empty")
    private List<
            @NotNull(message = "Transaction ID cannot be null")
            @Positive(message = "Transaction ID must be positive")
            Long
    > transactionIds;

    public CreateSettlementJobRequest() {
    }

    public List<Long> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(List<Long> transactionIds) {
        this.transactionIds = transactionIds;
    }
}