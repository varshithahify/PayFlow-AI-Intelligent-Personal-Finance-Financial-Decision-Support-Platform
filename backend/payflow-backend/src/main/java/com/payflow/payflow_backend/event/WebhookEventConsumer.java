package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.service.WebhookConfigService;
import com.payflow.payflow_backend.service.WebhookDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class WebhookEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(WebhookEventConsumer.class);

    private static final String TRANSACTION_EVENTS_TOPIC =
            "transaction-events";

    private static final String CONSUMER_GROUP =
            "webhook-group";

    private final WebhookConfigService webhookConfigService;

    private final WebhookDeliveryService webhookDeliveryService;

    public WebhookEventConsumer(
            WebhookConfigService webhookConfigService,
            WebhookDeliveryService webhookDeliveryService) {

        this.webhookConfigService =
                webhookConfigService;

        this.webhookDeliveryService =
                webhookDeliveryService;
    }

    @KafkaListener(
            topics = TRANSACTION_EVENTS_TOPIC,
            groupId = CONSUMER_GROUP)
    public void consume(TransactionEvent event) {

        logger.info(
                "Received webhook event: eventId={}, orgId={}, transactionId={}, eventType={}",
                event.getEventId(),
                event.getOrgId(),
                event.getTransactionId(),
                event.getEventType());

        List<WebhookConfig> webhooks =
                webhookConfigService.getActiveWebhooks(
                        event.getOrgId());

        if (webhooks.isEmpty()) {

            logger.debug(
                    "No active webhooks configured for orgId={}",
                    event.getOrgId());

            return;
        }

        for (WebhookConfig webhook : webhooks) {

            if (!isSubscribed(webhook, event)) {

                logger.debug(
                        "Skipping webhook id={} because it is not subscribed to eventType={}",
                        webhook.getId(),
                        event.getEventType());

                continue;
            }

            try {

                webhookDeliveryService.deliver(
                        webhook,
                        event);

                logger.info(
                        "Webhook delivered successfully: webhookId={}, eventId={}",
                        webhook.getId(),
                        event.getEventId());

            } catch (Exception exception) {

                logger.error(
                        "Webhook delivery failed: webhookId={}, eventId={}, endpoint={}",
                        webhook.getId(),
                        event.getEventId(),
                        webhook.getEndpointUrl(),
                        exception);

                throw exception;
            }
        }
    }

    private boolean isSubscribed(
            WebhookConfig webhook,
            TransactionEvent event) {

        String configuredEvents =
                webhook.getEvents();

        if (configuredEvents == null
                || configuredEvents.isBlank()) {

            return true;
        }

        String eventType =
                event.getEventType().name();

        return Arrays.stream(
                        configuredEvents.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .anyMatch(value ->
                        value.equalsIgnoreCase(eventType));
    }
}