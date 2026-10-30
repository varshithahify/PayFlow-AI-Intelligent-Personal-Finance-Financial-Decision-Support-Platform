package com.payflow.payflow_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ai_investigation_findings",
    indexes = {
        @Index(
            name = "idx_ai_findings_org_investigation",
            columnList = "org_id, investigation_id"
        ),
        @Index(
            name = "idx_ai_findings_org_transaction",
            columnList = "org_id, transaction_id"
        )
    }
)
public class AIInvestigationFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "likely_cause", nullable = false, length = 2000)
    private String likelyCause;

    @Column(name = "evidence", nullable = false, columnDefinition = "TEXT")
    private String evidence;

    @Column(
        name = "recommended_actions",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String recommendedActions;

    @Column(
        name = "limitations",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String limitations;

    @Column(name = "requires_human_review", nullable = false)
    private boolean requiresHumanReview = true;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AIInvestigationFinding() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getInvestigationId() {
        return investigationId;
    }

    public void setInvestigationId(Long investigationId) {
        this.investigationId = investigationId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getLikelyCause() {
        return likelyCause;
    }

    public void setLikelyCause(String likelyCause) {
        this.likelyCause = likelyCause;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(String recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public String getLimitations() {
        return limitations;
    }

    public void setLimitations(String limitations) {
        this.limitations = limitations;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public void setRequiresHumanReview(boolean requiresHumanReview) {
        this.requiresHumanReview = requiresHumanReview;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

