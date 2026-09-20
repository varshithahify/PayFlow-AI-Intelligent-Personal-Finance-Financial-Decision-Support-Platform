package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "investigations")
public class Investigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "reconciliation_record_id", nullable = false)
    private Long reconciliationRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestigationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestigationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 30)
    private InvestigationIssueType issueType;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Investigation() {
    }

    public Long getId() {
        return id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getReconciliationRecordId() {
        return reconciliationRecordId;
    }

    public InvestigationStatus getStatus() {
        return status;
    }

    public InvestigationPriority getPriority() {
        return priority;
    }

    public InvestigationIssueType getIssueType() {
        return issueType;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public void setReconciliationRecordId(Long reconciliationRecordId) {
        this.reconciliationRecordId = reconciliationRecordId;
    }

    public void setStatus(InvestigationStatus status) {
        this.status = status;
    }

    public void setPriority(InvestigationPriority priority) {
        this.priority = priority;
    }

    public void setIssueType(InvestigationIssueType issueType) {
        this.issueType = issueType;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}