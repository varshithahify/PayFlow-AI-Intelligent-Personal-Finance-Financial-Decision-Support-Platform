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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        webhookEventConsumer =
                new WebhookEventConsumer(
                        webhookConfigService,
                        webhookDeliveryService);
    }

    @Test
void shouldSkipDeliveryWhenWebhookIsNotSubscribedToEvent() {
    TransactionEvent event = new TransactionEvent(
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

    WebhookConfig webhook = new WebhookConfig(
            1L,
            "https://example.com/webhook",
            "secret",
            "FAILED",
            true
    );

    when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

    webhookEventConsumer.consume(event);

    verify(webhookConfigService).getActiveWebhooks(1L);
    verifyNoInteractions(webhookDeliveryService);
}

    @Test
void shouldDeliverEventWhenWebhookIsSubscribed() {
    TransactionEvent event = new TransactionEvent(
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

    WebhookConfig webhook = new WebhookConfig(
            1L,
            "https://example.com/webhook",
            "secret",
            "SUCCESS",
            true
    );

    when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

    webhookEventConsumer.consume(event);

    verify(webhookConfigService).getActiveWebhooks(1L);
    verify(webhookDeliveryService).deliver(webhook, event);
}

    @Test
void shouldDeliverEventWhenWebhookHasNoEventFilter() {
    TransactionEvent event = new TransactionEvent(
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

    WebhookConfig webhook = new WebhookConfig(
            1L,
            "https://example.com/webhook",
            "secret",
            "",
            true
    );

    when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

    webhookEventConsumer.consume(event);

    verify(webhookDeliveryService).deliver(webhook, event);
}

    @Test
void shouldDeliverEventToAllSubscribedWebhooks() {
    TransactionEvent event = new TransactionEvent(
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

    WebhookConfig webhook1 = new WebhookConfig(
            1L,
            "https://example.com/webhook-1",
            "secret-1",
            "SUCCESS",
            true
    );

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

    @Test
void shouldPropagateExceptionWhenWebhookDeliveryFails() {
    TransactionEvent event = new TransactionEvent(
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

    WebhookConfig webhook = new WebhookConfig(
            1L,
            "https://example.com/webhook",
            "secret",
            "SUCCESS",
            true
    );

    when(webhookConfigService.getActiveWebhooks(1L))
            .thenReturn(List.of(webhook));

    RuntimeException exception = new RuntimeException("Webhook endpoint unavailable");

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

    @Test
    void shouldSkipDeliveryWhenNoActiveWebhooksExist() {
        TransactionEvent event =
                new TransactionEvent(
                        100L,
                        10L,
                        1L,
                        new BigDecimal("500.00"),
                        "INR",
                        "UPI",
                        TransactionStatus.SUCCESS,
                        TransactionEventType.SUCCESS,
                        LocalDateTime.now());

        when(webhookConfigService.getActiveWebhooks(1L))
                .thenReturn(List.of());

        webhookEventConsumer.consume(event);

        verify(webhookConfigService)
                .getActiveWebhooks(1L);

        verifyNoInteractions(webhookDeliveryService);
    }
}