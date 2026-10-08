package com.payflow.payflow_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FraudScoreResponse {

    private double score;
    private String action;

    @JsonProperty("ml_score")
    private double mlScore;

    @JsonProperty("velocity_score")
    private double velocityScore;

    @JsonProperty("triggered_rules")
    private List<String> triggeredRules;

    public FraudScoreResponse() {
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
}