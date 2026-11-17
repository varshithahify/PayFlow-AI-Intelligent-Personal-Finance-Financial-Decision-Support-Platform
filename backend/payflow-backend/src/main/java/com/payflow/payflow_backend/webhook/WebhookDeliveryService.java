package com.payflow.payflow_backend.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.security.webhook.WebhookUrlValidator;
import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.event.TransactionEvent;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class WebhookDeliveryService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WebhookUrlValidator urlValidator;

    public WebhookDeliveryService(
            CloseableHttpClient webhookHttpClient,
            ObjectMapper objectMapper,
            WebhookUrlValidator urlValidator) {

        this.restClient = RestClient.builder()
                .requestFactory(
                        new HttpComponentsClientHttpRequestFactory(
                                webhookHttpClient))
                .build();

        this.objectMapper = objectMapper;
        this.urlValidator = urlValidator;
    }

    public void deliver(WebhookConfig webhookConfig, TransactionEvent event) {

        try {
            // Validate destination before making any outbound request.
            URI endpoint = urlValidator.validate(
                    webhookConfig.getEndpointUrl());

            String payload = objectMapper.writeValueAsString(event);

            var request = restClient.post()
                    .uri(endpoint)
                    .header(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE)
                    .header(
                            "X-PayFlow-Event",
                            event.getEventType().name())
                    .header(
                            "X-PayFlow-Event-Id",
                            event.getEventId().toString());

            String secret = webhookConfig.getSecretKey();

            if (secret != null && !secret.isBlank()) {
                String signature = generateSignature(payload, secret);

                request.header(
                        "X-PayFlow-Signature",
                        "sha256=" + signature);
            }

            request.body(payload)
                    .retrieve()
                    .toBodilessEntity();

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize webhook payload",
                    exception);

        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Webhook destination rejected by security validation",
                    exception);

        } catch (Exception exception) {
            // Do not include endpoint URLs, query parameters or credentials.
            throw new IllegalStateException(
                    "Webhook delivery failed",
                    exception);
        }
    }

    private String generateSignature(String payload, String secret) {

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256);

            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate webhook signature",
                    exception);
        }
    }
}