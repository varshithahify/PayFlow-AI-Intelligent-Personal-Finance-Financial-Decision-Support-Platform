package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionEvent {

    private UUID eventId;

    private Long transactionId;

    private Long userId;

    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private TransactionStatus status;

    private TransactionEventType eventType;

    private LocalDateTime timestamp;

    public TransactionEvent() {
    }

    public TransactionEvent(
            Long transactionId,
            Long userId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            TransactionStatus status,
            TransactionEventType eventType,
            LocalDateTime timestamp) {

        this.eventId = UUID.randomUUID();
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public TransactionEventType getEventType() {
        return eventType;
    }

    public void setEventType(TransactionEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}