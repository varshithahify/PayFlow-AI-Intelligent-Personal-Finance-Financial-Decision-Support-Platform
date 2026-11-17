package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.FailedEventRecord;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class FailedEventRecordRepositoryTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private FailedEventRecordRepository repository;

    private int insertEvent(
            UUID eventId,
            Long orgId,
            String eventType,
            LocalDateTime recordedAt
    ) {

        return repository.insertIfNotExists(
                eventId,
                null,
                orgId,
                eventType,
                "Test failure reason",
                recordedAt
        );
    }

    // --------------------------------------------------
    // EXISTING INSERTION TESTS
    // --------------------------------------------------

    @Test
    void shouldInsertNewFailedEvent() {

        UUID eventId = UUID.randomUUID();

        int inserted = insertEvent(
                eventId,
                null,
                "PAYMENT_FAILED",
                LocalDateTime.now()
        );

        assertEquals(1, inserted);
        assertTrue(repository.existsByEventId(eventId));
    }

    @Test
    void shouldReturnFalseWhenEventDoesNotExist() {

        UUID eventId = UUID.randomUUID();

        assertFalse(repository.existsByEventId(eventId));
    }

    @Test
    void shouldReturnTrueWhenEventAlreadyExists() {

        UUID eventId = UUID.randomUUID();

        insertEvent(
                eventId,
                null,
                "PAYMENT_FAILED",
                LocalDateTime.now()
        );

        assertTrue(repository.existsByEventId(eventId));
    }

    @Test
    void shouldIgnoreDuplicateEventInsertion() {

        UUID eventId = UUID.randomUUID();

        int firstInsert = insertEvent(
                eventId,
                null,
                "PAYMENT_FAILED",
                LocalDateTime.now()
        );

        int secondInsert = insertEvent(
                eventId,
                null,
                "PAYMENT_FAILED",
                LocalDateTime.now()
        );

        assertEquals(1, firstInsert);
        assertEquals(0, secondInsert);

        assertEquals(1, repository.count());
    }

    @Test
    void shouldAllowDifferentEventIds() {

        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();

        int firstInsert =
                insertEvent(
                        firstEventId,
                        null,
                        "PAYMENT_FAILED",
                        LocalDateTime.now()
                );

        int secondInsert =
                insertEvent(
                        secondEventId,
                        null,
                        "REFUND_FAILED",
                        LocalDateTime.now()
                );

        assertEquals(1, firstInsert);
        assertEquals(1, secondInsert);

        assertTrue(repository.existsByEventId(firstEventId));
        assertTrue(repository.existsByEventId(secondEventId));

        assertEquals(2, repository.count());
    }

    @Test
    void shouldPersistInsertedEventDetails() {

        UUID eventId = UUID.randomUUID();

        insertEvent(
                eventId,
                null,
                "PAYMENT_FAILED",
                LocalDateTime.now()
        );

        List<FailedEventRecord> records = repository.findAll();

        assertEquals(1, records.size());

        FailedEventRecord record = records.get(0);

        assertEquals(eventId, record.getEventId());
        assertEquals("PAYMENT_FAILED", record.getEventType());
        assertEquals("Test failure reason", record.getFailureReason());
        assertNotNull(record.getRecordedAt());
    }

    // --------------------------------------------------
    // EVENT TYPE FILTERING
    // --------------------------------------------------

    @Test
    void shouldFilterFailedEventsByEventType() {

        LocalDateTime now = LocalDateTime.now();

        insertEvent(
                UUID.randomUUID(),
                1L,
                "PAYMENT_FAILED",
                now
        );

        insertEvent(
                UUID.randomUUID(),
                1L,
                "REFUND_FAILED",
                now
        );

        Page<FailedEventRecord> result =
                repository.findFilteredEvents(
                        1L,
                        "PAYMENT_FAILED",
                        null,
                        null,
                        PageRequest.of(0, 20)
                );

        assertEquals(1, result.getTotalElements());

        assertEquals(
                "PAYMENT_FAILED",
                result.getContent().get(0).getEventType()
        );
    }

    // --------------------------------------------------
    // DATE RANGE FILTERING
    // --------------------------------------------------

    @Test
    void shouldFilterFailedEventsByDateRange() {

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 10, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 15, 23, 59);

        insertEvent(
                UUID.randomUUID(),
                1L,
                "PAYMENT_FAILED",
                LocalDateTime.of(2026, 11, 12, 10, 0)
        );

        insertEvent(
                UUID.randomUUID(),
                1L,
                "PAYMENT_FAILED",
                LocalDateTime.of(2026, 11, 18, 10, 0)
        );

        Page<FailedEventRecord> result =
                repository.findFilteredEvents(
                        1L,
                        null,
                        fromDate,
                        toDate,
                        PageRequest.of(0, 20)
                );

        assertEquals(1, result.getTotalElements());
    }

    // --------------------------------------------------
    // COMBINED FILTERING
    // --------------------------------------------------

    @Test
    void shouldCombineFailedEventFilters() {

        LocalDateTime fromDate =
                LocalDateTime.of(2026, 11, 10, 0, 0);

        LocalDateTime toDate =
                LocalDateTime.of(2026, 11, 15, 23, 59);

        insertEvent(
                UUID.randomUUID(),
                1L,
                "PAYMENT_FAILED",
                LocalDateTime.of(2026, 11, 12, 10, 0)
        );

        insertEvent(
                UUID.randomUUID(),
                1L,
                "REFUND_FAILED",
                LocalDateTime.of(2026, 11, 12, 10, 0)
        );

        insertEvent(
                UUID.randomUUID(),
                1L,
                "PAYMENT_FAILED",
                LocalDateTime.of(2026, 11, 18, 10, 0)
        );

        Page<FailedEventRecord> result =
                repository.findFilteredEvents(
                        1L,
                        "PAYMENT_FAILED",
                        fromDate,
                        toDate,
                        PageRequest.of(0, 20)
                );

        assertEquals(1, result.getTotalElements());

        assertEquals(
                "PAYMENT_FAILED",
                result.getContent().get(0).getEventType()
        );
    }

    // --------------------------------------------------
    // PAGINATION
    // --------------------------------------------------

    @Test
    void shouldReturnFailedEventsUsingPagination() {

        for (int i = 0; i < 5; i++) {
            insertEvent(
                    UUID.randomUUID(),
                    1L,
                    "PAYMENT_FAILED",
                    LocalDateTime.now()
            );
        }

        Page<FailedEventRecord> result =
                repository.findFilteredEvents(
                        1L,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 2)
                );

        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalPages());
    }
}