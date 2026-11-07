package com.payflow.payflow_backend.event;

import com.payflow.payflow_backend.entity.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEventTest {

    @Test
    void shouldCreateEmptyEventUsingDefaultConstructor() {

        TransactionEvent event = new TransactionEvent();

        assertNull(event.getEventId());
        assertNull(event.getTransactionId());
        assertNull(event.getUserId());
        assertNull(event.getOrgId());
        assertNull(event.getAmount());
        assertNull(event.getCurrency());
        assertNull(event.getPaymentMethod());
        assertNull(event.getStatus());
        assertNull(event.getEventType());
        assertNull(event.getTimestamp());
    }

    @Test
    void shouldInitializeAllFieldsUsingParameterizedConstructor() {

        Long transactionId = 100L;
        Long userId = 10L;
        Long orgId = 20L;
        BigDecimal amount = new BigDecimal("1250.50");
        String currency = "INR";
        String paymentMethod = "UPI";
        TransactionStatus status = TransactionStatus.SUCCESS;
        TransactionEventType eventType = TransactionEventType.CREATED;
        LocalDateTime timestamp =
                LocalDateTime.of(2026, 11, 4, 22, 30);

        TransactionEvent event = new TransactionEvent(
                transactionId,
                userId,
                orgId,
                amount,
                currency,
                paymentMethod,
                status,
                eventType,
                timestamp
        );

        assertNotNull(event.getEventId());
        assertEquals(transactionId, event.getTransactionId());
        assertEquals(userId, event.getUserId());
        assertEquals(orgId, event.getOrgId());
        assertEquals(amount, event.getAmount());
        assertEquals(currency, event.getCurrency());
        assertEquals(paymentMethod, event.getPaymentMethod());
        assertEquals(status, event.getStatus());
        assertEquals(eventType, event.getEventType());
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    void shouldGenerateUniqueEventIdsForSeparateEvents() {

        LocalDateTime timestamp = LocalDateTime.now();

        TransactionEvent first = new TransactionEvent(
                100L, 10L, 20L,
                new BigDecimal("100.00"),
                "INR", "UPI",
                TransactionStatus.SUCCESS,
                TransactionEventType.CREATED,
                timestamp
        );

        TransactionEvent second = new TransactionEvent(
                101L, 10L, 20L,
                new BigDecimal("200.00"),
                "INR", "CARD",
                TransactionStatus.PROCESSING,
                TransactionEventType.CREATED,
                timestamp
        );

        assertNotNull(first.getEventId());
        assertNotNull(second.getEventId());

        assertNotEquals(first.getEventId(), second.getEventId());
    }

    @Test
    void shouldSetAndGetEventId() {

        TransactionEvent event = new TransactionEvent();
        UUID eventId = UUID.randomUUID();

        event.setEventId(eventId);

        assertEquals(eventId, event.getEventId());
    }

    @Test
    void shouldSetAndGetTransactionId() {

        TransactionEvent event = new TransactionEvent();

        event.setTransactionId(200L);

        assertEquals(200L, event.getTransactionId());
    }

    @Test
    void shouldSetAndGetUserId() {

        TransactionEvent event = new TransactionEvent();

        event.setUserId(30L);

        assertEquals(30L, event.getUserId());
    }

    @Test
    void shouldSetAndGetOrganizationId() {

        TransactionEvent event = new TransactionEvent();

        event.setOrgId(40L);

        assertEquals(40L, event.getOrgId());
    }

    @Test
    void shouldSetAndGetAmount() {

        TransactionEvent event = new TransactionEvent();
        BigDecimal amount = new BigDecimal("999.99");

        event.setAmount(amount);

        assertEquals(amount, event.getAmount());
    }

    @Test
    void shouldSetAndGetCurrency() {

        TransactionEvent event = new TransactionEvent();

        event.setCurrency("USD");

        assertEquals("USD", event.getCurrency());
    }

    @Test
    void shouldSetAndGetPaymentMethod() {

        TransactionEvent event = new TransactionEvent();

        event.setPaymentMethod("CARD");

        assertEquals("CARD", event.getPaymentMethod());
    }

    @Test
    void shouldSetAndGetStatus() {

        TransactionEvent event = new TransactionEvent();

        event.setStatus(TransactionStatus.FAILED);

        assertEquals(TransactionStatus.FAILED, event.getStatus());
    }

    @Test
    void shouldSetAndGetEventType() {

        TransactionEvent event = new TransactionEvent();

        event.setEventType(TransactionEventType.CREATED);

        assertEquals(TransactionEventType.CREATED, event.getEventType());
    }

    @Test
    void shouldSetAndGetTimestamp() {

        TransactionEvent event = new TransactionEvent();

        LocalDateTime timestamp = LocalDateTime.now();

        event.setTimestamp(timestamp);

        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    void shouldAllowNullValuesThroughSetters() {

        TransactionEvent event = new TransactionEvent(
                100L,
                10L,
                20L,
                new BigDecimal("100.00"),
                "INR",
                "UPI",
                TransactionStatus.SUCCESS,
                TransactionEventType.CREATED,
                LocalDateTime.now()
        );

        event.setEventId(null);
        event.setTransactionId(null);
        event.setUserId(null);
        event.setOrgId(null);
        event.setAmount(null);
        event.setCurrency(null);
        event.setPaymentMethod(null);
        event.setStatus(null);
        event.setEventType(null);
        event.setTimestamp(null);

        assertNull(event.getEventId());
        assertNull(event.getTransactionId());
        assertNull(event.getUserId());
        assertNull(event.getOrgId());
        assertNull(event.getAmount());
        assertNull(event.getCurrency());
        assertNull(event.getPaymentMethod());
        assertNull(event.getStatus());
        assertNull(event.getEventType());
        assertNull(event.getTimestamp());
    }
}