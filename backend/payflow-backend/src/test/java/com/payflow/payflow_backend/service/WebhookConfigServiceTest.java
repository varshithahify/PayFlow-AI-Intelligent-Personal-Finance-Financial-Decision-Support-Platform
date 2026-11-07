package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.repository.WebhookConfigRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookConfigServiceTest {

    private static final Long ORG_ID = 10L;
    private static final Long WEBHOOK_ID = 100L;

    @Mock
    private WebhookConfigRepository webhookConfigRepository;

    @InjectMocks
    private WebhookConfigService webhookConfigService;

    private WebhookConfig webhookConfig;

    @BeforeEach
    void setUp() {

        webhookConfig = new WebhookConfig(
                ORG_ID,
                "https://example.com/webhook",
                "secret-key-123",
                "TRANSACTION_CREATED,TRANSACTION_FAILED",
                true
        );

        webhookConfig.setId(WEBHOOK_ID);
    }

    @Test
    void shouldSuccessfullyCreateWebhook() {

        when(webhookConfigRepository.save(any(WebhookConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WebhookConfig result = webhookConfigService.createWebhook(
                ORG_ID,
                "https://example.com/webhook",
                "secret-key-123",
                "TRANSACTION_CREATED"
        );

        assertNotNull(result);
        assertEquals(ORG_ID, result.getOrgId());
        assertEquals("https://example.com/webhook",
                result.getEndpointUrl());
        assertEquals("secret-key-123", result.getSecretKey());
        assertEquals("TRANSACTION_CREATED", result.getEvents());
        assertTrue(result.isActive());
        assertNotNull(result.getCreatedAt());

        verify(webhookConfigRepository).save(any(WebhookConfig.class));
    }

    @Test
    void shouldRetrieveAllWebhooksForOrganization() {

        List<WebhookConfig> webhooks = List.of(
                webhookConfig,
                new WebhookConfig(
                        ORG_ID,
                        "https://example.org/webhook",
                        "secret-456",
                        "TRANSACTION_FAILED",
                        false
                )
        );

        when(webhookConfigRepository
                .findByOrgIdOrderByCreatedAtDesc(ORG_ID))
                .thenReturn(webhooks);

        List<WebhookConfig> result =
                webhookConfigService.getWebhooks(ORG_ID);

        assertEquals(2, result.size());
        assertEquals(webhooks, result);

        verify(webhookConfigRepository)
                .findByOrgIdOrderByCreatedAtDesc(ORG_ID);
    }

    @Test
    void shouldReturnEmptyListWhenOrganizationHasNoWebhooks() {

        when(webhookConfigRepository
                .findByOrgIdOrderByCreatedAtDesc(ORG_ID))
                .thenReturn(List.of());

        List<WebhookConfig> result =
                webhookConfigService.getWebhooks(ORG_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSuccessfullyRetrieveWebhookById() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.of(webhookConfig));

        WebhookConfig result =
                webhookConfigService.getWebhook(ORG_ID, WEBHOOK_ID);

        assertNotNull(result);
        assertEquals(WEBHOOK_ID, result.getId());
        assertEquals(ORG_ID, result.getOrgId());

        verify(webhookConfigRepository)
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID);
    }

    @Test
    void shouldThrowExceptionWhenWebhookDoesNotExist() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> webhookConfigService.getWebhook(
                        ORG_ID,
                        WEBHOOK_ID
                )
        );
    }

    @Test
    void shouldRetrieveOnlyActiveWebhooks() {

        List<WebhookConfig> activeWebhooks =
                List.of(webhookConfig);

        when(webhookConfigRepository
                .findByOrgIdAndActiveTrueOrderByCreatedAtDesc(ORG_ID))
                .thenReturn(activeWebhooks);

        List<WebhookConfig> result =
                webhookConfigService.getActiveWebhooks(ORG_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());

        verify(webhookConfigRepository)
                .findByOrgIdAndActiveTrueOrderByCreatedAtDesc(ORG_ID);
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveWebhooksExist() {

        when(webhookConfigRepository
                .findByOrgIdAndActiveTrueOrderByCreatedAtDesc(ORG_ID))
                .thenReturn(List.of());

        List<WebhookConfig> result =
                webhookConfigService.getActiveWebhooks(ORG_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSuccessfullyUpdateWebhook() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.of(webhookConfig));

        when(webhookConfigRepository.save(any(WebhookConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WebhookConfig result = webhookConfigService.updateWebhook(
                ORG_ID,
                WEBHOOK_ID,
                "https://updated.example.com/webhook",
                "updated-secret",
                "TRANSACTION_SUCCESS",
                false
        );

        assertEquals(
                "https://updated.example.com/webhook",
                result.getEndpointUrl()
        );

        assertEquals("updated-secret", result.getSecretKey());
        assertEquals("TRANSACTION_SUCCESS", result.getEvents());
        assertFalse(result.isActive());

        verify(webhookConfigRepository)
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID);

        verify(webhookConfigRepository).save(webhookConfig);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingWebhook() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> webhookConfigService.updateWebhook(
                        ORG_ID,
                        WEBHOOK_ID,
                        "https://updated.example.com",
                        "secret",
                        "TRANSACTION_CREATED",
                        true
                )
        );

        verify(webhookConfigRepository, never())
                .save(any(WebhookConfig.class));
    }

    @Test
    void shouldSuccessfullyDeleteWebhook() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.of(webhookConfig));

        webhookConfigService.deleteWebhook(ORG_ID, WEBHOOK_ID);

        verify(webhookConfigRepository)
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID);

        verify(webhookConfigRepository).delete(webhookConfig);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingWebhook() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> webhookConfigService.deleteWebhook(
                        ORG_ID,
                        WEBHOOK_ID
                )
        );

        verify(webhookConfigRepository, never())
                .delete(any(WebhookConfig.class));
    }

    @Test
    void shouldPreserveOrganizationOwnershipWhenUpdatingWebhook() {

        when(webhookConfigRepository
                .findByOrgIdAndId(ORG_ID, WEBHOOK_ID))
                .thenReturn(Optional.of(webhookConfig));

        when(webhookConfigRepository.save(any(WebhookConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        webhookConfigService.updateWebhook(
                ORG_ID,
                WEBHOOK_ID,
                "https://updated.example.com",
                "new-secret",
                "TRANSACTION_FAILED",
                true
        );

        assertEquals(ORG_ID, webhookConfig.getOrgId());
    }
}