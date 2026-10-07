package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.repository.WebhookConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WebhookConfigService {

    private final WebhookConfigRepository webhookConfigRepository;

    public WebhookConfigService(
            WebhookConfigRepository webhookConfigRepository) {
        this.webhookConfigRepository = webhookConfigRepository;
    }

    @Transactional
    public WebhookConfig createWebhook(
            Long orgId,
            String endpointUrl,
            String secretKey,
            String events) {

        WebhookConfig webhookConfig = new WebhookConfig(
                orgId,
                endpointUrl,
                secretKey,
                events,
                true);

        return webhookConfigRepository.save(webhookConfig);
    }

    public List<WebhookConfig> getWebhooks(Long orgId) {
        return webhookConfigRepository
                .findByOrgIdOrderByCreatedAtDesc(orgId);
    }

    public WebhookConfig getWebhook(Long orgId, Long id) {
        return webhookConfigRepository
                .findByOrgIdAndId(orgId, id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Webhook configuration not found"));
    }

    public List<WebhookConfig> getActiveWebhooks(Long orgId) {
        return webhookConfigRepository
                .findByOrgIdAndActiveTrueOrderByCreatedAtDesc(orgId);
    }

    @Transactional
    public WebhookConfig updateWebhook(
            Long orgId,
            Long id,
            String endpointUrl,
            String secretKey,
            String events,
            boolean active) {

        WebhookConfig webhookConfig = getWebhook(orgId, id);

        webhookConfig.setEndpointUrl(endpointUrl);
        webhookConfig.setSecretKey(secretKey);
        webhookConfig.setEvents(events);
        webhookConfig.setActive(active);

        return webhookConfigRepository.save(webhookConfig);
    }

    @Transactional
    public void deleteWebhook(Long orgId, Long id) {
        WebhookConfig webhookConfig = getWebhook(orgId, id);
        webhookConfigRepository.delete(webhookConfig);
    }
}