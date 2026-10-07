package com.payflow.payflow_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.event.TransactionEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class WebhookDeliveryService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WebhookDeliveryService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {

        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public void deliver(
            WebhookConfig webhookConfig,
            TransactionEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            var request = restClient
                    .post()
                    .uri(webhookConfig.getEndpointUrl())
                    .header(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE)
                    .header(
                            "X-PayFlow-Event",
                            event.getEventType().name())
                    .header(
                            "X-PayFlow-Event-Id",
                            event.getEventId().toString());

            if (webhookConfig.getSecretKey() != null
                    && !webhookConfig.getSecretKey().isBlank()) {

                String signature =
                        generateSignature(
                                payload,
                                webhookConfig.getSecretKey());

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

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Webhook delivery failed for endpoint: "
                            + webhookConfig.getEndpointUrl(),
                    exception);
        }
    }

    private String generateSignature(
            String payload,
            String secretKey) {

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(
                            secretKey.getBytes(StandardCharsets.UTF_8),
                            HMAC_SHA256);

            mac.init(secretKeySpec);

            byte[] digest =
                    mac.doFinal(
                            payload.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate webhook signature",
                    exception);
        }
    }
}