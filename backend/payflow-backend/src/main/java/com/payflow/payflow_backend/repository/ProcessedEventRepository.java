package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(UUID eventId);

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_events (
                        event_id,
                        event_type,
                        processed_at
                    )
                    VALUES (
                        :eventId,
                        :eventType,
                        :processedAt
                    )
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfNotExists(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("processedAt") LocalDateTime processedAt);
}