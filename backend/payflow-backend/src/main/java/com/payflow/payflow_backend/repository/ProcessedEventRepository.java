package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ProcessedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Page<ProcessedEvent> findByOrgId(
            Long orgId,
            Pageable pageable
    );

    Optional<ProcessedEvent> findByEventIdAndOrgId(
            UUID eventId,
            Long orgId
    );

    long countByOrgId(Long orgId);

    long countByOrgIdAndProcessedAtAfter(
            Long orgId,
            LocalDateTime since
    );

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_events (
                        event_id,
                        event_type,
                        org_id,
                        processed_at
                    )
                    VALUES (
                        :eventId,
                        :eventType,
                        :orgId,
                        :processedAt
                    )
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfNotExists(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("orgId") Long orgId,
            @Param("processedAt") LocalDateTime processedAt
    );

    @Query("""
        SELECT p FROM ProcessedEvent p
        WHERE p.orgId = :orgId
        AND p.eventType = COALESCE(:eventType, p.eventType)
        AND p.processedAt >= COALESCE(:fromDate, p.processedAt)
        AND p.processedAt <= COALESCE(:toDate, p.processedAt)
        """)
Page<ProcessedEvent> findFilteredEvents(
        @Param("orgId") Long orgId,
        @Param("eventType") String eventType,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
);
}