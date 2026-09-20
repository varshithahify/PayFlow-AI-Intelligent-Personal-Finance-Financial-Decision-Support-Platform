package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_records")
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "external_record_id")
    private Long externalRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationStatus status;

    @Column(name = "internal_amount", precision = 15, scale = 2)
    private BigDecimal internalAmount;

    @Column(name = "external_amount", precision = 15, scale = 2)
    private BigDecimal externalAmount;

    @Column(name = "internal_status", length = 20)
    private String internalStatus;

    @Column(name = "external_status", length = 20)
    private String externalStatus;

    @Column(name = "mismatch_reason", length = 500)
    private String mismatchReason;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt;

    public ReconciliationRecord() {
    }

    public Long getId() {
        return id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getExternalRecordId() {
        return externalRecordId;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public BigDecimal getInternalAmount() {
        return internalAmount;
    }

    public BigDecimal getExternalAmount() {
        return externalAmount;
    }

    public String getInternalStatus() {
        return internalStatus;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public String getMismatchReason() {
        return mismatchReason;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public void setExternalRecordId(Long externalRecordId) {
        this.externalRecordId = externalRecordId;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public void setInternalAmount(BigDecimal internalAmount) {
        this.internalAmount = internalAmount;
    }

    public void setExternalAmount(BigDecimal externalAmount) {
        this.externalAmount = externalAmount;
    }

    public void setInternalStatus(String internalStatus) {
        this.internalStatus = internalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public void setMismatchReason(String mismatchReason) {
        this.mismatchReason = mismatchReason;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}