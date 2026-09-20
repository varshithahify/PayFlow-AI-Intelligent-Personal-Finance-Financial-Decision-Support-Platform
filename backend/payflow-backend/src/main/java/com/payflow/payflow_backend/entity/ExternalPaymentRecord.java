package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "external_payment_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_transaction_id",
                        columnNames = "external_transaction_id"
                )
        }
)
public class ExternalPaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "external_transaction_id",
            nullable = false,
            length = 100
    )
    private String externalTransactionId;

    @Column(name = "internal_transaction_id")
    private Long internalTransactionId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 50)
    private String gateway;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ExternalPaymentRecord() {
    }

    public ExternalPaymentRecord(
            String externalTransactionId,
            Long internalTransactionId,
            BigDecimal amount,
            String currency,
            String status,
            String gateway,
            LocalDateTime processedAt) {

        this.externalTransactionId = externalTransactionId;
        this.internalTransactionId = internalTransactionId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.gateway = gateway;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public Long getInternalTransactionId() {
        return internalTransactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getGateway() {
        return gateway;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public void setInternalTransactionId(Long internalTransactionId) {
        this.internalTransactionId = internalTransactionId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}