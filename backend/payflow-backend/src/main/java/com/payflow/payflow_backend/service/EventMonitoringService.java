package com.payflow.payflow_backend.service;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import com.payflow.payflow_backend.entity.ProcessedEvent;
import com.payflow.payflow_backend.exception.ResourceNotFoundException;
import com.payflow.payflow_backend.repository.FailedEventRecordRepository;
import com.payflow.payflow_backend.repository.ProcessedEventRepository;
import com.payflow.payflow_backend.security.TenantContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventMonitoringService {

    private final ProcessedEventRepository processedEventRepository;
    private final FailedEventRecordRepository failedEventRecordRepository;

    public EventMonitoringService(
            ProcessedEventRepository processedEventRepository,
            FailedEventRecordRepository failedEventRecordRepository) {

        this.processedEventRepository = processedEventRepository;
        this.failedEventRecordRepository = failedEventRecordRepository;
    }

    // EVENT OVERVIEW

    public Map<String, Long> getEventOverview() {

        Long orgId = TenantContext.requireOrgId();

        long processedCount =
                processedEventRepository.countByOrgId(orgId);

        long failedCount =
                failedEventRecordRepository.countByOrgId(orgId);

        return Map.of(
                "totalProcessedEvents", processedCount,
                "totalFailedEvents", failedCount,
                "totalEvents", processedCount + failedCount
        );
    }

    // PAGINATED PROCESSED EVENTS

    public Page<ProcessedEvent> getProcessedEvents(
            Pageable pageable) {

        return getProcessedEvents(
                pageable,
                null,
                null,
                null
        );
    }

    // PAGINATED AND FILTERED PROCESSED EVENTS

    public Page<ProcessedEvent> getProcessedEvents(
            Pageable pageable,
            String eventType,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        Long orgId = TenantContext.requireOrgId();

        validateDateRange(fromDate, toDate);

        String normalizedEventType =
                normalizeEventType(eventType);

        return processedEventRepository.findFilteredEvents(
                orgId,
                normalizedEventType,
                fromDate,
                toDate,
                pageable
        );
    }

    // PAGINATED FAILED EVENTS

    public Page<FailedEventRecord> getFailedEvents(
            Pageable pageable) {

        return getFailedEvents(
                pageable,
                null,
                null,
                null
        );
    }

    // PAGINATED AND FILTERED FAILED EVENTS

    public Page<FailedEventRecord> getFailedEvents(
            Pageable pageable,
            String eventType,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        Long orgId = TenantContext.requireOrgId();

        validateDateRange(fromDate, toDate);

        String normalizedEventType =
                normalizeEventType(eventType);

        return failedEventRecordRepository.findFilteredEvents(
                orgId,
                normalizedEventType,
                fromDate,
                toDate,
                pageable
        );
    }

    // PROCESSED EVENT DETAILS

    public ProcessedEvent getProcessedEvent(UUID eventId) {

        Long orgId = TenantContext.requireOrgId();

        return processedEventRepository
                .findByEventIdAndOrgId(eventId, orgId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Processed event not found: " + eventId
                        )
                );
    }

    // FAILED EVENT DETAILS

    public FailedEventRecord getFailedEvent(UUID eventId) {

        Long orgId = TenantContext.requireOrgId();

        return failedEventRecordRepository
                .findByEventIdAndOrgId(eventId, orgId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Failed event not found: " + eventId
                        )
                );
    }

    // EVENT COUNTS SINCE A GIVEN TIME

    public Map<String, Long> getEventCountsSince(
            LocalDateTime since) {

        Long orgId = TenantContext.requireOrgId();

        long processedCount =
                processedEventRepository
                        .countByOrgIdAndProcessedAtAfter(orgId, since);

        long failedCount =
                failedEventRecordRepository
                        .countByOrgIdAndRecordedAtAfter(orgId, since);

        return Map.of(
                "processedEvents", processedCount,
                "failedEvents", failedCount
        );
    }

    // DATE RANGE VALIDATION

    private void validateDateRange(
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new IllegalArgumentException(
                    "fromDate cannot be after toDate"
            );
        }
    }

    // EVENT TYPE NORMALIZATION

    private String normalizeEventType(String eventType) {

        if (eventType == null || eventType.isBlank()) {
            return null;
        }

        return eventType.trim();
    }
}
