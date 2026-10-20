package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.FailedEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FailedEventRecordRepository
        extends JpaRepository<FailedEventRecord, Long> {

    boolean existsByEventId(UUID eventId);

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
            @Param("recordedAt") LocalDateTime recordedAt);
}