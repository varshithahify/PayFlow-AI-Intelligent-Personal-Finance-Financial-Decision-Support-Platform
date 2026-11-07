package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ExternalPaymentRecordRepositoryTest {

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
    private ExternalPaymentRecordRepository repository;

    private ExternalPaymentRecord createRecord(
            String externalId,
            Long transactionId,
            Long orgId,
            LocalDateTime processedAt) {

        return new ExternalPaymentRecord(
                externalId,
                transactionId,
                orgId,
                new BigDecimal("1500.00"),
                "INR",
                "SUCCESS",
                "RAZORPAY",
                processedAt
        );
    }

    @Test
    void shouldSaveAndRetrieveExternalPaymentRecord() {

        ExternalPaymentRecord record = createRecord(
                "EXT-001", 101L, 1L, LocalDateTime.now()
        );

        ExternalPaymentRecord saved = repository.saveAndFlush(record);

        assertNotNull(saved.getId());
        assertEquals("EXT-001", saved.getExternalTransactionId());
        assertEquals(101L, saved.getInternalTransactionId());
        assertEquals(1L, saved.getOrgId());
        assertEquals(new BigDecimal("1500.00"), saved.getAmount());
        assertEquals("INR", saved.getCurrency());
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals("RAZORPAY", saved.getGateway());
    }

    @Test
    void shouldFindRecordByExternalTransactionId() {

        repository.saveAndFlush(
                createRecord(
                        "EXT-002", 102L, 1L, LocalDateTime.now()
                )
        );

        Optional<ExternalPaymentRecord> result =
                repository.findByExternalTransactionId("EXT-002");

        assertTrue(result.isPresent());
        assertEquals(
                "EXT-002",
                result.get().getExternalTransactionId()
        );
    }

    @Test
    void shouldReturnEmptyWhenExternalTransactionIdDoesNotExist() {

        Optional<ExternalPaymentRecord> result =
                repository.findByExternalTransactionId("UNKNOWN-ID");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnRecordsInDescendingProcessedAtOrder() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createRecord("EXT-OLD", 103L, 1L, now.minusHours(2)),
                createRecord("EXT-NEW", 103L, 1L, now),
                createRecord("EXT-MIDDLE", 103L, 1L, now.minusHours(1))
        ));

        List<ExternalPaymentRecord> records =
                repository
                        .findAllByOrgIdAndInternalTransactionIdOrderByProcessedAtDesc(
                                1L, 103L
                        );

        assertEquals(3, records.size());

        assertEquals("EXT-NEW",
                records.get(0).getExternalTransactionId());

        assertEquals("EXT-MIDDLE",
                records.get(1).getExternalTransactionId());

        assertEquals("EXT-OLD",
                records.get(2).getExternalTransactionId());
    }

    @Test
    void shouldFilterRecordsByOrganizationId() {

        repository.saveAndFlush(
                createRecord(
                        "EXT-ORG-1", 104L, 1L, LocalDateTime.now()
                )
        );

        List<ExternalPaymentRecord> records =
                repository
                        .findAllByOrgIdAndInternalTransactionIdOrderByProcessedAtDesc(
                                999L, 104L
                        );

        assertTrue(records.isEmpty());
    }

    @Test
    void shouldFilterRecordsByInternalTransactionId() {

        repository.saveAllAndFlush(List.of(
                createRecord("EXT-TX-1", 105L, 1L, LocalDateTime.now()),
                createRecord("EXT-TX-2", 106L, 1L, LocalDateTime.now())
        ));

        List<ExternalPaymentRecord> records =
                repository
                        .findAllByOrgIdAndInternalTransactionIdOrderByProcessedAtDesc(
                                1L, 105L
                        );

        assertEquals(1, records.size());
        assertEquals(
                105L,
                records.get(0).getInternalTransactionId()
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoMatchingRecordsExist() {

        List<ExternalPaymentRecord> records =
                repository
                        .findAllByOrgIdAndInternalTransactionIdOrderByProcessedAtDesc(
                                1L, 999L
                        );

        assertTrue(records.isEmpty());
    }
}