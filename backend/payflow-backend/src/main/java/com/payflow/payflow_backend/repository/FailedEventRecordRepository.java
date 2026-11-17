package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface FailedEventRecordRepository
        extends JpaRepository<FailedEventRecord, Long> {

    boolean existsByEventId(UUID eventId);

    Page<FailedEventRecord> findByOrgId(
            Long orgId,
            Pageable pageable
    );

    Optional<FailedEventRecord> findByEventIdAndOrgId(
            UUID eventId,
            Long orgId
    );

    long countByOrgId(Long orgId);

    long countByOrgIdAndRecordedAtAfter(
            Long orgId,
            LocalDateTime since
    );

    @Modifying
    @Query(
            value = """
                    INSERT INTO failed_event_records (
                        event_id,
                        transaction_id,
                        org_id,
                        event_type,
                        failure_reason,
                        recorded_at
                    )
                    VALUES (
                        :eventId,
                        :transactionId,
                        :orgId,
                        :eventType,
                        :failureReason,
                        :recordedAt
                    )
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfNotExists(
            @Param("eventId") UUID eventId,
            @Param("transactionId") Long transactionId,
            @Param("orgId") Long orgId,
            @Param("eventType") String eventType,
            @Param("failureReason") String failureReason,
            @Param("recordedAt") LocalDateTime recordedAt
    );

   @Query("""
        SELECT f FROM FailedEventRecord f
        WHERE f.orgId = :orgId
        AND f.eventType = COALESCE(:eventType, f.eventType)
        AND f.recordedAt >= COALESCE(:fromDate, f.recordedAt)
        AND f.recordedAt <= COALESCE(:toDate, f.recordedAt)
        """)
Page<FailedEventRecord> findFilteredEvents(
        @Param("orgId") Long orgId,
        @Param("eventType") String eventType,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
);
}