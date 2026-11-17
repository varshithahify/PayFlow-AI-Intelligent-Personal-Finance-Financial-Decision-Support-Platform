package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "settlement_job_transactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_job_transaction",
            columnNames = {"settlement_job_id", "transaction_id"}
        )
    }
)
public class SettlementJobTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_job_id", nullable = false)
    private Long settlementJobId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SettlementJobTransaction() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getSettlementJobId() { return settlementJobId; }
    public Long getTransactionId() { return transactionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setSettlementJobId(Long settlementJobId) { this.settlementJobId = settlementJobId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}