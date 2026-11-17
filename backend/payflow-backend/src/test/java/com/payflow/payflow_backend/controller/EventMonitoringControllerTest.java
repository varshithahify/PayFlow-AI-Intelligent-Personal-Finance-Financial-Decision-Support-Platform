package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import com.payflow.payflow_backend.entity.ProcessedEvent;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.security.TenantContext;
import com.payflow.payflow_backend.service.EventMonitoringService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventMonitoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventMonitoringService eventMonitoringService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final UUID EVENT_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // --------------------------------------------------
    // TEST DATA
    // --------------------------------------------------

    private ProcessedEvent createProcessedEvent() {

        return new ProcessedEvent(
                EVENT_ID,
                "TRANSACTION_SUCCESS",
                1L,
                LocalDateTime.now()
        );
    }

    private FailedEventRecord createFailedEvent() {

        FailedEventRecord event = new FailedEventRecord();

        event.setEventId(EVENT_ID);
        event.setTransactionId(100L);
        event.setOrgId(1L);
        event.setEventType("TRANSACTION_FAILED");
        event.setFailureReason("Processing failed");
        event.setRecordedAt(LocalDateTime.now());

        return event;
    }

    // --------------------------------------------------
    // 1. EVENT OVERVIEW
    // --------------------------------------------------

    @Test
    void getEventOverviewReturnsStatistics() throws Exception {

        when(eventMonitoringService.getEventOverview())
                .thenReturn(Map.of(
                        "totalProcessedEvents", 100L,
                        "totalFailedEvents", 10L,
                        "totalEvents", 110L
                ));

        mockMvc.perform(get("/api/events/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessedEvents").value(100))
                .andExpect(jsonPath("$.totalFailedEvents").value(10))
                .andExpect(jsonPath("$.totalEvents").value(110));
    }

    // --------------------------------------------------
    // 2. PROCESSED EVENTS
    // --------------------------------------------------

    @Test
    void getProcessedEventsReturnsPage() throws Exception {

        Page<ProcessedEvent> processedPage =
                new PageImpl<>(List.of(createProcessedEvent()));

        when(eventMonitoringService.getProcessedEvents(
                any(Pageable.class),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(processedPage);

        mockMvc.perform(get("/api/events/processed")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("TRANSACTION_SUCCESS"))
                .andExpect(jsonPath("$.content[0].orgId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --------------------------------------------------
    // 3. PROCESSED EVENT FILTERS
    // --------------------------------------------------

    @Test
    void getProcessedEventsAppliesFilters() throws Exception {

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 1, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 17, 23, 59);

        when(eventMonitoringService.getProcessedEvents(
                any(Pageable.class),
                eq("TRANSACTION_SUCCESS"),
                eq(fromDate),
                eq(toDate)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/api/events/processed")
                        .param("page", "0")
                        .param("size", "20")
                        .param("eventType", "TRANSACTION_SUCCESS")
                        .param("fromDate", "2026-11-01T00:00:00")
                        .param("toDate", "2026-11-17T23:59:00"))
                .andExpect(status().isOk());

        verify(eventMonitoringService).getProcessedEvents(
                any(Pageable.class),
                eq("TRANSACTION_SUCCESS"),
                eq(fromDate),
                eq(toDate)
        );
    }

    // --------------------------------------------------
    // 4. PROCESSED EVENTS INVALID PAGE SIZE
    // --------------------------------------------------

    @Test
    void getProcessedEventsRejectsInvalidPageSize() throws Exception {

        mockMvc.perform(get("/api/events/processed")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventMonitoringService);
    }

    // --------------------------------------------------
    // 5. PROCESSED EVENTS INVALID DATE FORMAT
    // --------------------------------------------------

    @Test
    void getProcessedEventsRejectsInvalidDateFormat() throws Exception {

        mockMvc.perform(get("/api/events/processed")
                        .param("fromDate", "invalid-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventMonitoringService);
    }

    // --------------------------------------------------
    // 6. PROCESSED EVENT DETAILS
    // --------------------------------------------------

    @Test
    void getProcessedEventReturnsDetails() throws Exception {

        when(eventMonitoringService.getProcessedEvent(EVENT_ID))
                .thenReturn(createProcessedEvent());

        mockMvc.perform(
                        get("/api/events/processed/{eventId}", EVENT_ID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType")
                        .value("TRANSACTION_SUCCESS"))
                .andExpect(jsonPath("$.orgId").value(1));
    }

    // --------------------------------------------------
    // 7. FAILED EVENTS
    // --------------------------------------------------

    @Test
    void getFailedEventsReturnsPage() throws Exception {

        Page<FailedEventRecord> failedPage =
                new PageImpl<>(List.of(createFailedEvent()));

        when(eventMonitoringService.getFailedEvents(
                any(Pageable.class),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(failedPage);

        mockMvc.perform(get("/api/events/failed")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("TRANSACTION_FAILED"))
                .andExpect(jsonPath("$.content[0].orgId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --------------------------------------------------
    // 8. FAILED EVENT FILTERS
    // --------------------------------------------------

    @Test
    void getFailedEventsAppliesFilters() throws Exception {

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 1, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 17, 23, 59);

        when(eventMonitoringService.getFailedEvents(
                any(Pageable.class),
                eq("TRANSACTION_FAILED"),
                eq(fromDate),
                eq(toDate)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/api/events/failed")
                        .param("page", "0")
                        .param("size", "20")
                        .param("eventType", "TRANSACTION_FAILED")
                        .param("fromDate", "2026-11-01T00:00:00")
                        .param("toDate", "2026-11-17T23:59:00"))
                .andExpect(status().isOk());

        verify(eventMonitoringService).getFailedEvents(
                any(Pageable.class),
                eq("TRANSACTION_FAILED"),
                eq(fromDate),
                eq(toDate)
        );
    }

    // --------------------------------------------------
    // 9. FAILED EVENTS INVALID PAGE SIZE
    // --------------------------------------------------

    @Test
    void getFailedEventsRejectsInvalidPageSize() throws Exception {

        mockMvc.perform(get("/api/events/failed")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventMonitoringService);
    }

    // --------------------------------------------------
    // 10. FAILED EVENTS INVALID DATE FORMAT
    // --------------------------------------------------

    @Test
    void getFailedEventsRejectsInvalidDateFormat() throws Exception {

        mockMvc.perform(get("/api/events/failed")
                        .param("toDate", "invalid-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventMonitoringService);
    }

    // --------------------------------------------------
    // 11. FAILED EVENT DETAILS
    // --------------------------------------------------

    @Test
    void getFailedEventReturnsDetails() throws Exception {

        when(eventMonitoringService.getFailedEvent(EVENT_ID))
                .thenReturn(createFailedEvent());

        mockMvc.perform(
                        get("/api/events/failed/{eventId}", EVENT_ID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType")
                        .value("TRANSACTION_FAILED"))
                .andExpect(jsonPath("$.failureReason")
                        .value("Processing failed"))
                .andExpect(jsonPath("$.orgId").value(1));
    }

    // --------------------------------------------------
    // 12. INVALID PROCESSED EVENT UUID
    // --------------------------------------------------

    @Test
    void getProcessedEventRejectsInvalidUUID() throws Exception {

        mockMvc.perform(
                        get("/api/events/processed/{eventId}", "invalid-uuid")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventMonitoringService);
    }

    // --------------------------------------------------
    // 13. INVALID FAILED EVENT UUID
    // --------------------------------------------------

    @Test
    void getFailedEventRejectsInvalidUUID() throws Exception {

        mockMvc.perform(
                        get("/api/events/failed/{eventId}", "invalid-uuid")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventMonitoringService);
    }
}