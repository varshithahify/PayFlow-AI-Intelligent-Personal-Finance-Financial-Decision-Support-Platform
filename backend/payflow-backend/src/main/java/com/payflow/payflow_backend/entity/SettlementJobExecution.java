package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "settlement_job_executions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_job_execution_attempt",
            columnNames = {"settlement_job_id", "attempt_number"}
        )
    }
)
public class SettlementJobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_job_id", nullable = false)
    private Long settlementJobId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementExecutionStatus status;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount = 0;

    @Column(name = "discrepancy_count", nullable = false)
    private Integer discrepancyCount = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public SettlementJobExecution() {
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }

        if (status == null) {
            status = SettlementExecutionStatus.RUNNING;
        }

        if (processedCount == null) {
            processedCount = 0;
        }

        if (discrepancyCount == null) {
            discrepancyCount = 0;
        }
    }

    public Long getId() { return id; }
    public Long getSettlementJobId() { return settlementJobId; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public SettlementExecutionStatus getStatus() { return status; }
    public Integer getProcessedCount() { return processedCount; }
    public Integer getDiscrepancyCount() { return discrepancyCount; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setId(Long id) { this.id = id; }
    public void setSettlementJobId(Long settlementJobId) { this.settlementJobId = settlementJobId; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public void setStatus(SettlementExecutionStatus status) { this.status = status; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
    public void setDiscrepancyCount(Integer discrepancyCount) { this.discrepancyCount = discrepancyCount; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}