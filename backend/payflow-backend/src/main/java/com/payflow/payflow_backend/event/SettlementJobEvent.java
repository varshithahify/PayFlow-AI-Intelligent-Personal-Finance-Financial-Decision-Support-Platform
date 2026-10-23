package com.payflow.payflow_backend.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a settlement job lifecycle event published through Kafka.
 *
 * This class defines the event payload. It does not execute settlement
 * processing or reconcile payment records by itself.
 */
public class SettlementJobEvent {

    private UUID eventId;

    private String jobId;

    private Long orgId;

    private SettlementJobEventType eventType;

    private Integer transactionCount;

    private Integer discrepancyCount;

    private String details;

    private LocalDateTime occurredAt;

    public SettlementJobEvent() {
    }

    public SettlementJobEvent(
            UUID eventId,
            String jobId,
            Long orgId,
            SettlementJobEventType eventType,
            Integer transactionCount,
            Integer discrepancyCount,
            String details,
            LocalDateTime occurredAt) {

        this.eventId = eventId;
        this.jobId = jobId;
        this.orgId = orgId;
        this.eventType = eventType;
        this.transactionCount = transactionCount;
        this.discrepancyCount = discrepancyCount;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public SettlementJobEventType getEventType() {
        return eventType;
    }

    public void setEventType(SettlementJobEventType eventType) {
        this.eventType = eventType;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Integer getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(Integer discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}