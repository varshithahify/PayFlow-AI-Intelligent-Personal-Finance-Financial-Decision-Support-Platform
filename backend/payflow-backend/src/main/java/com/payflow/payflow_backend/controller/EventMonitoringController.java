package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import com.payflow.payflow_backend.entity.ProcessedEvent;
import com.payflow.payflow_backend.service.EventMonitoringService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Validated
public class EventMonitoringController {

    private final EventMonitoringService eventMonitoringService;

    public EventMonitoringController(
            EventMonitoringService eventMonitoringService) {
        this.eventMonitoringService = eventMonitoringService;
    }

    // --------------------------------------------------
    // EVENT OVERVIEW
    // GET /api/events/overview
    // --------------------------------------------------

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Long>> getEventOverview() {

        return ResponseEntity.ok(
                eventMonitoringService.getEventOverview()
        );
    }

    // --------------------------------------------------
    // GET PROCESSED EVENTS WITH FILTERS
    // GET /api/events/processed
    // --------------------------------------------------

    @GetMapping("/processed")
    public ResponseEntity<Page<ProcessedEvent>> getProcessedEvents(

            @RequestParam(defaultValue = "0")
            @Min(0) int page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,

            @RequestParam(required = false)
            String eventType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "processedAt")
        );

        return ResponseEntity.ok(
                eventMonitoringService.getProcessedEvents(
                        pageable,
                        eventType,
                        fromDate,
                        toDate
                )
        );
    }

    // --------------------------------------------------
    // GET FAILED EVENTS WITH FILTERS
    // GET /api/events/failed
    // --------------------------------------------------

    @GetMapping("/failed")
    public ResponseEntity<Page<FailedEventRecord>> getFailedEvents(

            @RequestParam(defaultValue = "0")
            @Min(0) int page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,

            @RequestParam(required = false)
            String eventType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "recordedAt")
        );

        return ResponseEntity.ok(
                eventMonitoringService.getFailedEvents(
                        pageable,
                        eventType,
                        fromDate,
                        toDate
                )
        );
    }

    // --------------------------------------------------
    // GET PROCESSED EVENT DETAILS
    // GET /api/events/processed/{eventId}
    // --------------------------------------------------

    @GetMapping("/processed/{eventId}")
    public ResponseEntity<ProcessedEvent> getProcessedEvent(
            @PathVariable UUID eventId) {

        return ResponseEntity.ok(
                eventMonitoringService.getProcessedEvent(eventId)
        );
    }

    // --------------------------------------------------
    // GET FAILED EVENT DETAILS
    // GET /api/events/failed/{eventId}
    // --------------------------------------------------

    @GetMapping("/failed/{eventId}")
    public ResponseEntity<FailedEventRecord> getFailedEvent(
            @PathVariable UUID eventId) {

        return ResponseEntity.ok(
                eventMonitoringService.getFailedEvent(eventId)
        );
    }
}
