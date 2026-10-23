package com.payflow.payflow_backend.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FraudAlertEvent {

    private UUID eventId;
    private Long transactionId;
    private Long orgId;
    private String action;
    private double riskScore;
    private double mlScore;
    private double velocityScore;
    private List<String> triggeredRules;
    private LocalDateTime occurredAt;

    public FraudAlertEvent() {
    }

    public FraudAlertEvent(
            UUID eventId,
            Long transactionId,
            Long orgId,
            String action,
            double riskScore,
            double mlScore,
            double velocityScore,
            List<String> triggeredRules,
            LocalDateTime occurredAt) {

        this.eventId = eventId;
        this.transactionId = transactionId;
        this.orgId = orgId;
        this.action = action;
        this.riskScore = riskScore;
        this.mlScore = mlScore;
        this.velocityScore = velocityScore;
        this.triggeredRules = triggeredRules;
        this.occurredAt = occurredAt;
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

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public double getMlScore() {
        return mlScore;
    }

    public void setMlScore(double mlScore) {
        this.mlScore = mlScore;
    }

    public double getVelocityScore() {
        return velocityScore;
    }

    public void setVelocityScore(double velocityScore) {
        this.velocityScore = velocityScore;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(List<String> triggeredRules) {
        this.triggeredRules = triggeredRules;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}