package com.payflow.payflow_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FraudScoreRequest {

    @JsonProperty("TransactionAmt")
    private double transactionAmt;

    private Double card1;
    private Double card2;
    private Double addr1;
    private Double dist1;
    private Double c1;
    private Double c2;
    private Double v1;
    private Double v2;
    private Double v3;

    @JsonProperty("card_hash")
    private String cardHash;
    private String email;

    public FraudScoreRequest() {
    }

    public double getTransactionAmt() {
        return transactionAmt;
    }

    public void setTransactionAmt(double transactionAmt) {
        this.transactionAmt = transactionAmt;
    }

    public Double getCard1() {
        return card1;
    }

    public void setCard1(Double card1) {
        this.card1 = card1;
    }

    public Double getCard2() {
        return card2;
    }

    public void setCard2(Double card2) {
        this.card2 = card2;
    }

    public Double getAddr1() {
        return addr1;
    }

    public void setAddr1(Double addr1) {
        this.addr1 = addr1;
    }

    public Double getDist1() {
        return dist1;
    }

    public void setDist1(Double dist1) {
        this.dist1 = dist1;
    }

    public Double getC1() {
        return c1;
    }

    public void setC1(Double c1) {
        this.c1 = c1;
    }

    public Double getC2() {
        return c2;
    }

    public void setC2(Double c2) {
        this.c2 = c2;
    }

    public Double getV1() {
        return v1;
    }

    public void setV1(Double v1) {
        this.v1 = v1;
    }

    public Double getV2() {
        return v2;
    }

    public void setV2(Double v2) {
        this.v2 = v2;
    }

    public Double getV3() {
        return v3;
    }

    public void setV3(Double v3) {
        this.v3 = v3;
    }

    public String getCardHash() {
        return cardHash;
    }

    public void setCardHash(String cardHash) {
        this.cardHash = cardHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}