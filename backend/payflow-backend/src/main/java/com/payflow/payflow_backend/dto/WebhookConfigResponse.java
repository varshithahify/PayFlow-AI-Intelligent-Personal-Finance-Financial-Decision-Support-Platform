package com.payflow.payflow_backend.dto;

import com.payflow.payflow_backend.entity.WebhookConfig;

import java.time.LocalDateTime;

public record WebhookConfigResponse(
        Long id,
        Long orgId,
        String endpointUrl,
        String events,
        boolean active,
        LocalDateTime createdAt
) {

    public static WebhookConfigResponse from(WebhookConfig webhook) {
        return new WebhookConfigResponse(
                webhook.getId(),
                webhook.getOrgId(),
                webhook.getEndpointUrl(),
                webhook.getEvents(),
                webhook.isActive(),
                webhook.getCreatedAt()
        );
    }
}