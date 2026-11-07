package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.service.WebhookConfigService;
import com.payflow.payflow_backend.service.WebhookDeliveryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookEventConsumerTest {

    @Mock
    private WebhookConfigService webhookConfigService;

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    private WebhookEventConsumer webhookEventConsumer;

    @BeforeEach
    void setUp() {

        webhookEventConsumer = new WebhookEventConsumer(
            webhookConfigService,
            webhookDeliveryService
        );
    }

    // --------------------------------------------------
    // 1. Skip delivery when event is not subscribed
    // --------------------------------------------------

    @Test
    void shouldSkipDeliveryWhenWebhookIsNotSubscribedToEvent() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook("FAILED");

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookConfigService).getActiveWebhooks(1L);

        verifyNoInteractions(webhookDeliveryService);
    }

    // --------------------------------------------------
    // 2. Deliver event when subscribed
    // --------------------------------------------------

    @Test
    void shouldDeliverEventWhenWebhookIsSubscribed() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook("SUCCESS");

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 3. Empty event filter
    // --------------------------------------------------

    @Test
    void shouldDeliverEventWhenWebhookHasNoEventFilter() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook("");

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 4. Deliver to all subscribed webhooks
    // --------------------------------------------------

    @Test
    void shouldDeliverEventToAllSubscribedWebhooks() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook1 = createWebhook("SUCCESS");

        WebhookConfig webhook2 = new WebhookConfig(
            1L,
            "https://example.com/webhook-2",
            "secret-2",
            "SUCCESS",
            true
        );

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook1, webhook2));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook1, event);

        verify(webhookDeliveryService).deliver(webhook2, event);

        verifyNoMoreInteractions(webhookDeliveryService);
    }

    // --------------------------------------------------
    // 5. Propagate delivery exception
    // --------------------------------------------------

    @Test
    void shouldPropagateExceptionWhenWebhookDeliveryFails() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook("SUCCESS");

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        RuntimeException exception =
            new RuntimeException("Webhook endpoint unavailable");

        doThrow(exception)
            .when(webhookDeliveryService)
            .deliver(webhook, event);

        RuntimeException thrownException = assertThrows(
            RuntimeException.class,
            () -> webhookEventConsumer.consume(event)
        );

        assertSame(exception, thrownException);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 6. No active webhooks
    // --------------------------------------------------

    @Test
    void shouldSkipDeliveryWhenNoActiveWebhooksExist() {

        TransactionEvent event = createEvent();

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of());

        assertDoesNotThrow(
            () -> webhookEventConsumer.consume(event)
        );

        verify(webhookConfigService).getActiveWebhooks(1L);

        verifyNoInteractions(webhookDeliveryService);
    }

    // --------------------------------------------------
    // 7. Null event filter
    // --------------------------------------------------

    @Test
    void shouldDeliverEventWhenWebhookEventFilterIsNull() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook(null);

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 8. Multiple comma-separated event subscriptions
    // --------------------------------------------------

    @Test
    void shouldDeliverEventWhenEventTypeExistsInMultipleSubscriptions() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook(
            "FAILED,SUCCESS,CREATED"
        );

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 9. Case-insensitive subscription matching
    // --------------------------------------------------

    @Test
    void shouldMatchEventSubscriptionCaseInsensitively() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook("success");

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 10. Ignore whitespace around subscriptions
    // --------------------------------------------------

    @Test
    void shouldIgnoreWhitespaceAroundConfiguredEventTypes() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook(
            " FAILED , SUCCESS , CREATED "
        );

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // 11. Skip unmatched webhook and deliver matching webhook
    // --------------------------------------------------

    @Test
    void shouldDeliverOnlyToMatchingSubscribedWebhooks() {

        TransactionEvent event = createEvent();

        WebhookConfig subscribedWebhook =
            createWebhook("SUCCESS");

        WebhookConfig unsubscribedWebhook =
            new WebhookConfig(
                1L,
                "https://example.com/webhook-failed",
                "secret-2",
                "FAILED",
                true
            );

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(
                List.of(
                    unsubscribedWebhook,
                    subscribedWebhook
                )
            );

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService)
            .deliver(subscribedWebhook, event);

        verify(webhookDeliveryService, never())
            .deliver(unsubscribedWebhook, event);

        verifyNoMoreInteractions(webhookDeliveryService);
    }

    // --------------------------------------------------
    // 12. Blank event filter
    // --------------------------------------------------

    @Test
    void shouldDeliverEventWhenWebhookEventFilterIsBlank() {

        TransactionEvent event = createEvent();

        WebhookConfig webhook = createWebhook("   ");

        when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

        webhookEventConsumer.consume(event);

        verify(webhookDeliveryService).deliver(webhook, event);
    }

    // --------------------------------------------------
    // Helper: Create transaction event
    // --------------------------------------------------

    private TransactionEvent createEvent() {

        return new TransactionEvent(
            100L,
            10L,
            1L,
            new BigDecimal("500.00"),
            "INR",
            "UPI",
            TransactionStatus.SUCCESS,
            TransactionEventType.SUCCESS,
            LocalDateTime.now()
        );
    }

    // --------------------------------------------------
    // Helper: Create webhook configuration
    // --------------------------------------------------

    private WebhookConfig createWebhook(String events) {

        return new WebhookConfig(
            1L,
            "https://example.com/webhook",
            "secret",
            events,
            true
        );
    }
}