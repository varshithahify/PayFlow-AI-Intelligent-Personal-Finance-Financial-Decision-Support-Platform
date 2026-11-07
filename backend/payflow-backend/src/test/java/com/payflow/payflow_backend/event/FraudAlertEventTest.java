package com.payflow.payflow_backend.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FraudAlertEventTest {

    @Test
    void shouldCreateEmptyEventUsingDefaultConstructor() {

        FraudAlertEvent event = new FraudAlertEvent();

        assertNull(event.getEventId());
        assertNull(event.getTransactionId());
        assertNull(event.getOrgId());
        assertNull(event.getAction());
        assertEquals(0.0, event.getRiskScore());
        assertEquals(0.0, event.getMlScore());
        assertEquals(0.0, event.getVelocityScore());
        assertNull(event.getTriggeredRules());
        assertNull(event.getOccurredAt());
    }

    @Test
    void shouldInitializeAllFieldsUsingParameterizedConstructor() {

        UUID eventId = UUID.randomUUID();
        Long transactionId = 100L;
        Long orgId = 20L;
        String action = "BLOCK";

        double riskScore = 0.95;
        double mlScore = 0.91;
        double velocityScore = 0.87;

        List<String> triggeredRules =
                List.of("HIGH_VELOCITY", "SUSPICIOUS_DEVICE");

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 11, 4, 22, 30);

        FraudAlertEvent event = new FraudAlertEvent(
                eventId,
                transactionId,
                orgId,
                action,
                riskScore,
                mlScore,
                velocityScore,
                triggeredRules,
                occurredAt
        );

        assertEquals(eventId, event.getEventId());
        assertEquals(transactionId, event.getTransactionId());
        assertEquals(orgId, event.getOrgId());
        assertEquals(action, event.getAction());
        assertEquals(riskScore, event.getRiskScore());
        assertEquals(mlScore, event.getMlScore());
        assertEquals(velocityScore, event.getVelocityScore());
        assertSame(triggeredRules, event.getTriggeredRules());
        assertEquals(occurredAt, event.getOccurredAt());
    }

    @Test
    void shouldUpdateEventId() {

        FraudAlertEvent event = new FraudAlertEvent();
        UUID eventId = UUID.randomUUID();

        event.setEventId(eventId);

        assertEquals(eventId, event.getEventId());
    }

    @Test
    void shouldUpdateTransactionId() {

        FraudAlertEvent event = new FraudAlertEvent();

        event.setTransactionId(200L);

        assertEquals(200L, event.getTransactionId());
    }

    @Test
    void shouldUpdateOrganizationId() {

        FraudAlertEvent event = new FraudAlertEvent();

        event.setOrgId(30L);

        assertEquals(30L, event.getOrgId());
    }

    @Test
    void shouldUpdateAction() {

        FraudAlertEvent event = new FraudAlertEvent();

        event.setAction("FLAG");

        assertEquals("FLAG", event.getAction());
    }

    @Test
    void shouldUpdateRiskScore() {

        FraudAlertEvent event = new FraudAlertEvent();

        event.setRiskScore(0.75);

        assertEquals(0.75, event.getRiskScore());
    }

    @Test
    void shouldUpdateMlScore() {

        FraudAlertEvent event = new FraudAlertEvent();

        event.setMlScore(0.82);

        assertEquals(0.82, event.getMlScore());
    }

    @Test
    void shouldUpdateVelocityScore() {

        FraudAlertEvent event = new FraudAlertEvent();

        event.setVelocityScore(0.68);

        assertEquals(0.68, event.getVelocityScore());
    }

    @Test
    void shouldUpdateTriggeredRules() {

        FraudAlertEvent event = new FraudAlertEvent();

        List<String> triggeredRules =
                List.of("UNUSUAL_LOCATION", "HIGH_AMOUNT");

        event.setTriggeredRules(triggeredRules);

        assertSame(triggeredRules, event.getTriggeredRules());
    }

    @Test
    void shouldUpdateOccurrenceTime() {

        FraudAlertEvent event = new FraudAlertEvent();

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 11, 4, 23, 0);

        event.setOccurredAt(occurredAt);

        assertEquals(occurredAt, event.getOccurredAt());
    }

    @Test
    void shouldAllowNullValuesThroughSetters() {

        FraudAlertEvent event = new FraudAlertEvent(
                UUID.randomUUID(),
                100L,
                20L,
                "BLOCK",
                0.95,
                0.91,
                0.87,
                List.of("HIGH_VELOCITY"),
                LocalDateTime.now()
        );

        event.setEventId(null);
        event.setTransactionId(null);
        event.setOrgId(null);
        event.setAction(null);
        event.setTriggeredRules(null);
        event.setOccurredAt(null);

        assertNull(event.getEventId());
        assertNull(event.getTransactionId());
        assertNull(event.getOrgId());
        assertNull(event.getAction());
        assertNull(event.getTriggeredRules());
        assertNull(event.getOccurredAt());
    }
}