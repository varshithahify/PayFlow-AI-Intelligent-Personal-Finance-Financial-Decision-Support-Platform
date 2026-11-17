package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import com.payflow.payflow_backend.entity.ProcessedEvent;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.repository.FailedEventRecordRepository;
import com.payflow.payflow_backend.repository.ProcessedEventRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventMonitoringServiceTest {

    private ProcessedEventRepository processedEventRepository;
    private FailedEventRecordRepository failedEventRecordRepository;

    private EventMonitoringService service;

    private static final Long ORG_ID = 10L;

    private static final UUID EVENT_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {

        TenantContext.setOrgId(ORG_ID);

        processedEventRepository =
                mock(ProcessedEventRepository.class);

        failedEventRecordRepository =
                mock(FailedEventRecordRepository.class);

        service = new EventMonitoringService(
                processedEventRepository,
                failedEventRecordRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ProcessedEvent createProcessedEvent() {

        return new ProcessedEvent(
                EVENT_ID,
                "TRANSACTION_SUCCESS",
                ORG_ID,
                LocalDateTime.now()
        );
    }

    private FailedEventRecord createFailedEvent() {

        FailedEventRecord event = new FailedEventRecord();

        event.setEventId(EVENT_ID);
        event.setTransactionId(100L);
        event.setOrgId(ORG_ID);
        event.setEventType("TRANSACTION_FAILED");
        event.setFailureReason("Processing failed");
        event.setRecordedAt(LocalDateTime.now());

        return event;
    }

    // --------------------------------------------------
    // EVENT OVERVIEW
    // --------------------------------------------------

    @Test
    void shouldReturnEventOverview() {

        when(processedEventRepository.countByOrgId(ORG_ID))
                .thenReturn(100L);

        when(failedEventRecordRepository.countByOrgId(ORG_ID))
                .thenReturn(10L);

        Map<String, Long> result =
                service.getEventOverview();

        assertEquals(100L, result.get("totalProcessedEvents"));
        assertEquals(10L, result.get("totalFailedEvents"));
        assertEquals(110L, result.get("totalEvents"));

        verify(processedEventRepository)
                .countByOrgId(ORG_ID);

        verify(failedEventRecordRepository)
                .countByOrgId(ORG_ID);
    }

    // --------------------------------------------------
    // PROCESSED EVENTS
    // --------------------------------------------------

    @Test
    void shouldReturnPaginatedProcessedEvents() {

        Pageable pageable = PageRequest.of(0, 20);

        Page<ProcessedEvent> expected =
                new PageImpl<>(
                        List.of(createProcessedEvent()),
                        pageable,
                        1
                );

        when(processedEventRepository.findFilteredEvents(
                eq(ORG_ID),
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(expected);

        Page<ProcessedEvent> result =
                service.getProcessedEvents(pageable);

        assertEquals(1, result.getTotalElements());

        assertEquals(
                EVENT_ID,
                result.getContent().get(0).getEventId()
        );

        verify(processedEventRepository)
                .findFilteredEvents(
                        eq(ORG_ID),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(pageable)
                );
    }

    @Test
    void shouldFilterProcessedEventsByEventTypeAndDateRange() {

        Pageable pageable = PageRequest.of(0, 20);

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 1, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 17, 23, 59);

        Page<ProcessedEvent> expected =
                new PageImpl<>(
                        List.of(createProcessedEvent()),
                        pageable,
                        1
                );

        when(processedEventRepository.findFilteredEvents(
                ORG_ID,
                "TRANSACTION_SUCCESS",
                fromDate,
                toDate,
                pageable
        )).thenReturn(expected);

        Page<ProcessedEvent> result =
                service.getProcessedEvents(
                        pageable,
                        "TRANSACTION_SUCCESS",
                        fromDate,
                        toDate
                );

        assertEquals(1, result.getTotalElements());

        verify(processedEventRepository).findFilteredEvents(
                ORG_ID,
                "TRANSACTION_SUCCESS",
                fromDate,
                toDate,
                pageable
        );
    }

    @Test
    void shouldNormalizeProcessedEventType() {

        Pageable pageable = PageRequest.of(0, 20);

        when(processedEventRepository.findFilteredEvents(
                eq(ORG_ID),
                eq("TRANSACTION_SUCCESS"),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        service.getProcessedEvents(
                pageable,
                "  TRANSACTION_SUCCESS  ",
                null,
                null
        );

        verify(processedEventRepository).findFilteredEvents(
                ORG_ID,
                "TRANSACTION_SUCCESS",
                null,
                null,
                pageable
        );
    }

    @Test
    void shouldReturnProcessedEventByEventId() {

        ProcessedEvent event = createProcessedEvent();

        when(processedEventRepository.findByEventIdAndOrgId(
                EVENT_ID, ORG_ID))
                .thenReturn(Optional.of(event));

        ProcessedEvent result =
                service.getProcessedEvent(EVENT_ID);

        assertEquals(EVENT_ID, result.getEventId());
        assertEquals(ORG_ID, result.getOrgId());

        verify(processedEventRepository)
                .findByEventIdAndOrgId(EVENT_ID, ORG_ID);
    }

    @Test
    void shouldThrowWhenProcessedEventDoesNotExist() {

        when(processedEventRepository.findByEventIdAndOrgId(
                EVENT_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getProcessedEvent(EVENT_ID)
        );
    }

    // --------------------------------------------------
    // FAILED EVENTS
    // --------------------------------------------------

    @Test
    void shouldReturnPaginatedFailedEvents() {

        Pageable pageable = PageRequest.of(0, 20);

        Page<FailedEventRecord> expected =
                new PageImpl<>(
                        List.of(createFailedEvent()),
                        pageable,
                        1
                );

        when(failedEventRecordRepository.findFilteredEvents(
                eq(ORG_ID),
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(expected);

        Page<FailedEventRecord> result =
                service.getFailedEvents(pageable);

        assertEquals(1, result.getTotalElements());

        assertEquals(
                EVENT_ID,
                result.getContent().get(0).getEventId()
        );

        verify(failedEventRecordRepository)
                .findFilteredEvents(
                        eq(ORG_ID),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(pageable)
                );
    }

    @Test
    void shouldFilterFailedEventsByEventTypeAndDateRange() {

        Pageable pageable = PageRequest.of(0, 20);

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 1, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 17, 23, 59);

        Page<FailedEventRecord> expected =
                new PageImpl<>(
                        List.of(createFailedEvent()),
                        pageable,
                        1
                );

        when(failedEventRecordRepository.findFilteredEvents(
                ORG_ID,
                "TRANSACTION_FAILED",
                fromDate,
                toDate,
                pageable
        )).thenReturn(expected);

        Page<FailedEventRecord> result =
                service.getFailedEvents(
                        pageable,
                        "TRANSACTION_FAILED",
                        fromDate,
                        toDate
                );

        assertEquals(1, result.getTotalElements());

        verify(failedEventRecordRepository).findFilteredEvents(
                ORG_ID,
                "TRANSACTION_FAILED",
                fromDate,
                toDate,
                pageable
        );
    }

    @Test
    void shouldReturnFailedEventByEventId() {

        FailedEventRecord event = createFailedEvent();

        when(failedEventRecordRepository.findByEventIdAndOrgId(
                EVENT_ID, ORG_ID))
                .thenReturn(Optional.of(event));

        FailedEventRecord result =
                service.getFailedEvent(EVENT_ID);

        assertEquals(EVENT_ID, result.getEventId());
        assertEquals(ORG_ID, result.getOrgId());

        verify(failedEventRecordRepository)
                .findByEventIdAndOrgId(EVENT_ID, ORG_ID);
    }

    @Test
    void shouldThrowWhenFailedEventDoesNotExist() {

        when(failedEventRecordRepository.findByEventIdAndOrgId(
                EVENT_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getFailedEvent(EVENT_ID)
        );
    }

    // --------------------------------------------------
    // DATE RANGE VALIDATION
    // --------------------------------------------------

    @Test
    void shouldRejectInvalidProcessedEventDateRange() {

        Pageable pageable = PageRequest.of(0, 20);

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 17, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 1, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getProcessedEvents(
                        pageable,
                        null,
                        fromDate,
                        toDate
                )
        );

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void shouldRejectInvalidFailedEventDateRange() {

        Pageable pageable = PageRequest.of(0, 20);

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 17, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 1, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getFailedEvents(
                        pageable,
                        null,
                        fromDate,
                        toDate
                )
        );

        verifyNoInteractions(failedEventRecordRepository);
    }

    // --------------------------------------------------
    // EVENT COUNTS SINCE TIMESTAMP
    // --------------------------------------------------

    @Test
    void shouldReturnEventCountsSinceTimestamp() {

        LocalDateTime since =
                LocalDateTime.now().minusHours(24);

        when(processedEventRepository
                .countByOrgIdAndProcessedAtAfter(ORG_ID, since))
                .thenReturn(50L);

        when(failedEventRecordRepository
                .countByOrgIdAndRecordedAtAfter(ORG_ID, since))
                .thenReturn(5L);

        Map<String, Long> result =
                service.getEventCountsSince(since);

        assertEquals(50L, result.get("processedEvents"));
        assertEquals(5L, result.get("failedEvents"));

        verify(processedEventRepository)
                .countByOrgIdAndProcessedAtAfter(ORG_ID, since);

        verify(failedEventRecordRepository)
                .countByOrgIdAndRecordedAtAfter(ORG_ID, since);
    }

    // --------------------------------------------------
    // TENANT ISOLATION
    // --------------------------------------------------

    @Test
    void shouldRejectRequestWhenTenantContextIsMissing() {

        TenantContext.clear();

        assertThrows(
                IllegalStateException.class,
                () -> service.getEventOverview()
        );

        verifyNoInteractions(processedEventRepository);
        verifyNoInteractions(failedEventRecordRepository);
    }
}