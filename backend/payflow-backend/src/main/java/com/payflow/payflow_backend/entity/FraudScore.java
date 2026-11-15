package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_scores")
public class FraudScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private Double score;

    @Column(name = "velocity_score")
    private Double velocityScore;

    @Column(name = "geo_score")
    private Double geoScore;

    @Column(name = "device_score")
    private Double deviceScore;

    @Column(name = "ml_score")
    private Double mlScore;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_decision", length = 20)
    private String reviewDecision;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    @PrePersist
    protected void onCreate() {
        if (scoredAt == null) {
            scoredAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getVelocityScore() {
        return velocityScore;
    }

    public void setVelocityScore(Double velocityScore) {
        this.velocityScore = velocityScore;
    }

    public Double getMlScore() {
        return mlScore;
    }

    public void setMlScore(Double mlScore) {
        this.mlScore = mlScore;
    }

    public String getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(String triggeredRules) {
        this.triggeredRules = triggeredRules;
    }

    public LocalDateTime getScoredAt() {
        return scoredAt;
    }

    public Long getReviewedBy() {
    return reviewedBy;
}

public void setReviewedBy(Long reviewedBy) {
    this.reviewedBy = reviewedBy;
}

public String getReviewDecision() {
    return reviewDecision;
}

public void setReviewDecision(String reviewDecision) {
    this.reviewDecision = reviewDecision;
}
}