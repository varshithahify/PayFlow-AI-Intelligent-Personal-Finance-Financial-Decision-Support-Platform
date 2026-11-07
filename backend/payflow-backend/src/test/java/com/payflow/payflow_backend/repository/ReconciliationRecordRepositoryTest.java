package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ReconciliationRecord;
import com.payflow.payflow_backend.entity.ReconciliationStatus;

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
class ReconciliationRecordRepositoryTest {

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
    private ReconciliationRecordRepository repository;

    private ReconciliationRecord createRecord(
            ReconciliationStatus status,
            LocalDateTime reconciledAt) {

        ReconciliationRecord record = new ReconciliationRecord();

        record.setTransactionId(null);
        record.setOrgId(1L);
        record.setExternalRecordId(null);
        record.setStatus(status);
        record.setInternalAmount(new BigDecimal("1500.00"));
        record.setExternalAmount(new BigDecimal("1500.00"));
        record.setInternalStatus("SUCCESS");
        record.setExternalStatus("SUCCESS");
        record.setMismatchReason(null);
        record.setReconciledAt(reconciledAt);

        return record;
    }

    @Test
    void shouldFindLatestRecordByTransactionId() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        now.minusHours(2)
                ),
                createRecord(
                        ReconciliationStatus.MISMATCH,
                        now
                ),
                createRecord(
                        ReconciliationStatus.MISSING_EXTERNAL,
                        now.minusHours(1)
                )
        ));

        Optional<ReconciliationRecord> result =
                repository.findTopByTransactionIdOrderByReconciledAtDesc(
                        null
                );

        assertTrue(result.isPresent());
        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.get().getStatus()
        );
    }

    @Test
    void shouldFindRecordsByStatus() {

        repository.saveAndFlush(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        LocalDateTime.now()
                )
        );

        List<ReconciliationRecord> results =
                repository.findByStatusOrderByReconciledAtDesc(
                        ReconciliationStatus.MATCHED
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                record -> record.getStatus()
                        == ReconciliationStatus.MATCHED
        ));
    }

    @Test
    void shouldFindRecordByIdAndOrganization() {

        ReconciliationRecord saved = repository.saveAndFlush(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        LocalDateTime.now()
                )
        );

        Optional<ReconciliationRecord> result =
                repository.findByIdAndOrgId(saved.getId(), 1L);

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void shouldNotFindRecordForDifferentOrganization() {

        ReconciliationRecord saved = repository.saveAndFlush(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        LocalDateTime.now()
                )
        );

        Optional<ReconciliationRecord> result =
                repository.findByIdAndOrgId(saved.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindLatestRecordByOrganizationAndTransactionId() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        now.minusHours(1)
                ),
                createRecord(
                        ReconciliationStatus.MISMATCH,
                        now
                )
        ));

        Optional<ReconciliationRecord> result =
                repository.findTopByOrgIdAndTransactionIdOrderByReconciledAtDesc(
                        1L,
                        null
                );

        assertTrue(result.isPresent());
        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.get().getStatus()
        );
    }

    @Test
    void shouldFindRecordsByOrganizationAndStatus() {

        repository.saveAndFlush(
                createRecord(
                        ReconciliationStatus.MISSING_INTERNAL,
                        LocalDateTime.now()
                )
        );

        List<ReconciliationRecord> results =
                repository.findByOrgIdAndStatusOrderByReconciledAtDesc(
                        1L,
                        ReconciliationStatus.MISSING_INTERNAL
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                record -> record.getOrgId().equals(1L)
                        && record.getStatus()
                        == ReconciliationStatus.MISSING_INTERNAL
        ));
    }

    @Test
    void shouldFindRecordByIdOrganizationAndTransactionId() {

        ReconciliationRecord saved = repository.saveAndFlush(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        LocalDateTime.now()
                )
        );

        Optional<ReconciliationRecord> result =
                repository.findByIdAndOrgIdAndTransactionId(
                        saved.getId(),
                        1L,
                        null
                );

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void shouldFindRecordByIdOrganizationAndExternalRecordId() {

        ReconciliationRecord saved = repository.saveAndFlush(
                createRecord(
                        ReconciliationStatus.MATCHED,
                        LocalDateTime.now()
                )
        );

        Optional<ReconciliationRecord> result =
                repository.findByIdAndOrgIdAndExternalRecordId(
                        saved.getId(),
                        1L,
                        null
                );

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }
}