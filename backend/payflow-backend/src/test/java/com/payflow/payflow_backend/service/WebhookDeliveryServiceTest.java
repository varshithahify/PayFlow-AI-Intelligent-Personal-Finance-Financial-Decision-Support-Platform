package com.payflow.payflow_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.WebhookConfig;
import com.payflow.payflow_backend.event.TransactionEvent;
import com.payflow.payflow_backend.event.TransactionEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WebhookDeliveryService webhookDeliveryService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper()
                .findAndRegisterModules();

        when(restClientBuilder.build())
                .thenReturn(restClient);

        webhookDeliveryService =
                new WebhookDeliveryService(
                        restClientBuilder,
                        objectMapper);
    }

    @Test
    void shouldDeliverWebhookWithSignature() {

        WebhookConfig webhookConfig =
                new WebhookConfig(
                        1L,
                        "https://example.com/webhook",
                        "test-secret",
                        "SUCCESS,FAILED",
                        true);

        UUID eventId = UUID.randomUUID();

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

        event.setEventId(eventId);

        when(restClient.post())
                .thenReturn(requestBodyUriSpec);

        when(requestBodyUriSpec.uri(
                webhookConfig.getEndpointUrl()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.header(
                anyString(),
                anyString()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.body(anyString()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.ok().build());

        webhookDeliveryService.deliver(
                webhookConfig,
                event);

        verify(restClient).post();

        verify(requestBodyUriSpec)
                .uri(webhookConfig.getEndpointUrl());

        verify(requestBodySpec)
                .header(
                        "Content-Type",
                        "application/json");

        verify(requestBodySpec)
                .header(
                        "X-PayFlow-Event",
                        "SUCCESS");

        verify(requestBodySpec)
                .header(
                        "X-PayFlow-Event-Id",
                        eventId.toString());

        verify(requestBodySpec)
                .header(
                        eq("X-PayFlow-Signature"),
                        startsWith("sha256="));

        verify(requestBodySpec)
                .body(anyString());

        verify(requestBodySpec)
                .retrieve();

        verify(responseSpec)
                .toBodilessEntity();
    }

    @Test
    void shouldDeliverWebhookWithoutSignatureWhenSecretIsBlank() {

        WebhookConfig webhookConfig =
                new WebhookConfig(
                        1L,
                        "https://example.com/webhook",
                        " ",
                        "SUCCESS",
                        true);

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

        when(restClient.post())
                .thenReturn(requestBodyUriSpec);

        when(requestBodyUriSpec.uri(
                webhookConfig.getEndpointUrl()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.header(
                anyString(),
                anyString()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.body(anyString()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.ok().build());

        webhookDeliveryService.deliver(
                webhookConfig,
                event);

        verify(requestBodySpec, never())
                .header(
                        eq("X-PayFlow-Signature"),
                        anyString());

        verify(responseSpec)
                .toBodilessEntity();
    }
}