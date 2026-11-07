package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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

@SpringBootTest
@Testcontainers
class ExternalPaymentRecordRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("payflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private ExternalPaymentRecordRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveExternalPaymentRecord() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-001",
                100L,
                1L,
                new BigDecimal("1500.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        ExternalPaymentRecord savedRecord = repository.saveAndFlush(record);

        assertNotNull(savedRecord.getId());

        Optional<ExternalPaymentRecord> retrievedRecord =
                repository.findByExternalTransactionId("EXT-001");

        assertTrue(retrievedRecord.isPresent());
        assertEquals("EXT-001",
                retrievedRecord.get().getExternalTransactionId());
        assertEquals(1L, retrievedRecord.get().getOrgId());
    }

    @Test
    void shouldPreventAccessToAnotherOrganizationsPaymentRecords() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-TENANT-001",
                200L,
                1L,
                new BigDecimal("2500.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        repository.saveAndFlush(record);

        List<ExternalPaymentRecord> records =
                repository.findAllByOrgIdAndInternalTransactionIdOrderByProcessedAtDesc(
                        2L,
                        200L
                );

        assertTrue(records.isEmpty());
    }

    @Test
    void shouldRejectDuplicateExternalTransactionId() {

        ExternalPaymentRecord firstRecord = new ExternalPaymentRecord(
                "EXT-DUPLICATE-001",
                300L,
                1L,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        ExternalPaymentRecord duplicateRecord = new ExternalPaymentRecord(
                "EXT-DUPLICATE-001",
                301L,
                1L,
                new BigDecimal("2000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_B",
                LocalDateTime.now()
        );

        repository.saveAndFlush(firstRecord);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicateRecord)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithInvalidOrganizationId() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-INVALID-ORG",
                400L,
                999999L,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullOrganizationId() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-NULL-ORG",
                500L,
                null,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullExternalTransactionId() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                null,
                600L,
                1L,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullAmount() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-NULL-AMOUNT",
                601L,
                1L,
                null,
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullCurrency() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-NULL-CURRENCY",
                602L,
                1L,
                new BigDecimal("1000.00"),
                null,
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullStatus() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-NULL-STATUS",
                603L,
                1L,
                new BigDecimal("1000.00"),
                "INR",
                null,
                "GATEWAY_A",
                LocalDateTime.now()
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullGateway() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-NULL-GATEWAY",
                604L,
                1L,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                null,
                LocalDateTime.now()
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldRejectPaymentRecordWithNullProcessedAt() {

        ExternalPaymentRecord record = new ExternalPaymentRecord(
                "EXT-NULL-PROCESSED-AT",
                605L,
                1L,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                null
        );

        assertThrows(
                Exception.class,
                () -> repository.saveAndFlush(record)
        );
    }

    @Test
    void shouldReturnRecordsInNewestFirstOrderForCorrectOrganization() {

        // Create a valid second organization.
        Long secondOrgId = jdbcTemplate.queryForObject(
                "INSERT INTO organizations (name) VALUES (?) RETURNING id",
                Long.class,
                "Test Organization 2"
        );

        assertNotNull(secondOrgId);

        ExternalPaymentRecord olderRecord = new ExternalPaymentRecord(
                "EXT-ORDER-001",
                700L,
                1L,
                new BigDecimal("1000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now().minusHours(2)
        );

        ExternalPaymentRecord newerRecord = new ExternalPaymentRecord(
                "EXT-ORDER-002",
                700L,
                1L,
                new BigDecimal("2000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_A",
                LocalDateTime.now()
        );

        ExternalPaymentRecord anotherOrgRecord = new ExternalPaymentRecord(
                "EXT-ORDER-003",
                700L,
                secondOrgId,
                new BigDecimal("3000.00"),
                "INR",
                "SUCCESS",
                "GATEWAY_B",
                LocalDateTime.now().plusHours(1)
        );

        repository.saveAndFlush(olderRecord);
        repository.saveAndFlush(newerRecord);
        repository.saveAndFlush(anotherOrgRecord);

        List<ExternalPaymentRecord> records =
                repository.findAllByOrgIdAndInternalTransactionIdOrderByProcessedAtDesc(
                        1L,
                        700L
                );

        assertEquals(2, records.size());

        assertEquals(
                "EXT-ORDER-002",
                records.get(0).getExternalTransactionId()
        );

        assertEquals(
                "EXT-ORDER-001",
                records.get(1).getExternalTransactionId()
        );

        assertTrue(
                records.stream().allMatch(
                        record -> record.getOrgId().equals(1L)
                )
        );
    }
}