package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.FailedEventRecord;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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

    private int insertEvent(UUID eventId, String eventType) {

        return repository.insertIfNotExists(
                eventId,
                null,
                null,
                eventType,
                "Test failure reason",
                LocalDateTime.now()
        );
    }

    @Test
    void shouldInsertNewFailedEvent() {

        UUID eventId = UUID.randomUUID();

        int inserted = insertEvent(eventId, "PAYMENT_FAILED");

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

        insertEvent(eventId, "PAYMENT_FAILED");

        assertTrue(repository.existsByEventId(eventId));
    }

    @Test
    void shouldIgnoreDuplicateEventInsertion() {

        UUID eventId = UUID.randomUUID();

        int firstInsert = insertEvent(eventId, "PAYMENT_FAILED");

        int secondInsert = insertEvent(eventId, "PAYMENT_FAILED");

        assertEquals(1, firstInsert);
        assertEquals(0, secondInsert);

        assertEquals(1, repository.count());
    }

    @Test
    void shouldAllowDifferentEventIds() {

        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();

        int firstInsert =
                insertEvent(firstEventId, "PAYMENT_FAILED");

        int secondInsert =
                insertEvent(secondEventId, "REFUND_FAILED");

        assertEquals(1, firstInsert);
        assertEquals(1, secondInsert);

        assertTrue(repository.existsByEventId(firstEventId));
        assertTrue(repository.existsByEventId(secondEventId));

        assertEquals(2, repository.count());
    }

    @Test
    void shouldPersistInsertedEventDetails() {

        UUID eventId = UUID.randomUUID();

        insertEvent(eventId, "PAYMENT_FAILED");

        List<FailedEventRecord> records = repository.findAll();

        assertEquals(1, records.size());

        FailedEventRecord record = records.get(0);

        assertEquals(eventId, record.getEventId());
        assertEquals("PAYMENT_FAILED", record.getEventType());
        assertEquals("Test failure reason", record.getFailureReason());
        assertNotNull(record.getRecordedAt());
    }
}