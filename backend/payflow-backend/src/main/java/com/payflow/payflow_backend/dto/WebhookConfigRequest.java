package com.payflow.payflow_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class WebhookConfigRequest {

    @NotBlank(message = "Endpoint URL is required")
    private String endpointUrl;

    private String secretKey;

    private String events;

    public WebhookConfigRequest() {
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }
}