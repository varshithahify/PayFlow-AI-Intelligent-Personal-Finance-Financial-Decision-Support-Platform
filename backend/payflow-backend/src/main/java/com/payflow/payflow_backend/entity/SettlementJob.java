package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_jobs")
public class SettlementJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 36)
    private String jobId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementJobStatus status;

    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount = 0;

    @Column(name = "discrepancy_count", nullable = false)
    private Integer discrepancyCount = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public SettlementJob() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = SettlementJobStatus.PENDING;
        }

        if (processedCount == null) {
            processedCount = 0;
        }

        if (discrepancyCount == null) {
            discrepancyCount = 0;
        }
    }

    public Long getId() { return id; }
    public String getJobId() { return jobId; }
    public Long getOrgId() { return orgId; }
    public SettlementJobStatus getStatus() { return status; }
    public Integer getTotalTransactions() { return totalTransactions; }
    public Integer getProcessedCount() { return processedCount; }
    public Integer getDiscrepancyCount() { return discrepancyCount; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setId(Long id) { this.id = id; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public void setStatus(SettlementJobStatus status) { this.status = status; }
    public void setTotalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
    public void setDiscrepancyCount(Integer discrepancyCount) { this.discrepancyCount = discrepancyCount; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}