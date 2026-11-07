package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.repository.ProcessedEventRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ProcessedEventService processedEventService;

    private final UUID eventId =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private final String eventType = "TRANSACTION_CREATED";

    private final Long orgId = 10L;

    @Test
    void shouldMarkEventAsProcessedWhenOneRowInserted() {

        when(processedEventRepository.insertIfNotExists(
                eq(eventId),
                eq(eventType),
                eq(orgId),
                any(LocalDateTime.class)
        )).thenReturn(1);

        boolean result = processedEventService.tryMarkAsProcessed(
                eventId,
                eventType,
                orgId
        );

        assertTrue(result);

        verify(processedEventRepository).insertIfNotExists(
                eq(eventId),
                eq(eventType),
                eq(orgId),
                any(LocalDateTime.class)
        );
    }

    @Test
    void shouldReturnFalseWhenEventAlreadyProcessed() {

        when(processedEventRepository.insertIfNotExists(
                eq(eventId),
                eq(eventType),
                eq(orgId),
                any(LocalDateTime.class)
        )).thenReturn(0);

        boolean result = processedEventService.tryMarkAsProcessed(
                eventId,
                eventType,
                orgId
        );

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenMoreThanOneRowInserted() {

        when(processedEventRepository.insertIfNotExists(
                eq(eventId),
                eq(eventType),
                eq(orgId),
                any(LocalDateTime.class)
        )).thenReturn(2);

        boolean result = processedEventService.tryMarkAsProcessed(
                eventId,
                eventType,
                orgId
        );

        assertFalse(result);
    }

    @Test
    void shouldPassCorrectArgumentsAndCurrentTimestamp() {

        ArgumentCaptor<LocalDateTime> timestampCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);

        LocalDateTime beforeCall = LocalDateTime.now();

        when(processedEventRepository.insertIfNotExists(
                eq(eventId),
                eq(eventType),
                eq(orgId),
                any(LocalDateTime.class)
        )).thenReturn(1);

        processedEventService.tryMarkAsProcessed(
                eventId,
                eventType,
                orgId
        );

        LocalDateTime afterCall = LocalDateTime.now();

        verify(processedEventRepository).insertIfNotExists(
                eq(eventId),
                eq(eventType),
                eq(orgId),
                timestampCaptor.capture()
        );

        LocalDateTime capturedTimestamp = timestampCaptor.getValue();

        assertNotNull(capturedTimestamp);

        assertFalse(capturedTimestamp.isBefore(beforeCall));
        assertFalse(capturedTimestamp.isAfter(afterCall));
    }
}