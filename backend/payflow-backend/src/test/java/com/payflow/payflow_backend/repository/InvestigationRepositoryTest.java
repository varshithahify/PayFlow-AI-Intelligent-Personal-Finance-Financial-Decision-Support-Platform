package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class InvestigationRepositoryTest {

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
    private InvestigationRepository repository;

    private Investigation createInvestigation(
            InvestigationStatus status,
            InvestigationPriority priority,
            LocalDateTime createdAt) {

        Investigation investigation = new Investigation();

        investigation.setOrgId(1L);
        investigation.setTransactionId(null);
        investigation.setReconciliationRecordId(null);
        investigation.setStatus(status);
        investigation.setPriority(priority);
        investigation.setIssueType(
                InvestigationIssueType.AMOUNT_MISMATCH
        );
        investigation.setSummary("Test investigation");
        investigation.setCreatedAt(createdAt);
        investigation.setUpdatedAt(createdAt);

        return investigation;
    }

    @Test
    void shouldFindInvestigationsByStatus() {

        repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.HIGH,
                        LocalDateTime.now()
                )
        );

        List<Investigation> results =
                repository.findByStatusOrderByCreatedAtDesc(
                        InvestigationStatus.OPEN
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                i -> i.getStatus() == InvestigationStatus.OPEN
        ));
    }

    @Test
    void shouldFindInvestigationsByPriority() {

        repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.CRITICAL,
                        LocalDateTime.now()
                )
        );

        List<Investigation> results =
                repository.findByPriorityOrderByCreatedAtDesc(
                        InvestigationPriority.CRITICAL
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                i -> i.getPriority() == InvestigationPriority.CRITICAL
        ));
    }

    @Test
    void shouldFindInvestigationsByTransactionId() {

        repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.HIGH,
                        LocalDateTime.now()
                )
        );

        List<Investigation> results =
                repository.findByTransactionIdOrderByCreatedAtDesc(null);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(
                i -> i.getTransactionId() == null
        ));
    }

    @Test
    void shouldFindLatestInvestigationByTransactionId() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.LOW,
                        now.minusHours(2)
                ),
                createInvestigation(
                        InvestigationStatus.INVESTIGATING,
                        InvestigationPriority.MEDIUM,
                        now
                ),
                createInvestigation(
                        InvestigationStatus.RESOLVED,
                        InvestigationPriority.HIGH,
                        now.minusHours(1)
                )
        ));

        Optional<Investigation> result =
                repository.findTopByTransactionIdOrderByCreatedAtDesc(null);

        assertTrue(result.isPresent());
        assertEquals(
                InvestigationStatus.INVESTIGATING,
                result.get().getStatus()
        );
    }

    @Test
    void shouldFindLatestInvestigationByReconciliationRecordId() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.LOW,
                        now.minusHours(1)
                ),
                createInvestigation(
                        InvestigationStatus.RESOLVED,
                        InvestigationPriority.HIGH,
                        now
                )
        ));

        Optional<Investigation> result =
                repository.findTopByReconciliationRecordIdOrderByCreatedAtDesc(
                        null
                );

        assertTrue(result.isPresent());
        assertEquals(
                InvestigationStatus.RESOLVED,
                result.get().getStatus()
        );
    }

    @Test
    void shouldFindInvestigationByIdAndOrganization() {

        Investigation saved = repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.MEDIUM,
                        LocalDateTime.now()
                )
        );

        Optional<Investigation> result =
                repository.findByIdAndOrgId(saved.getId(), 1L);

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void shouldNotFindInvestigationForDifferentOrganization() {

        Investigation saved = repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.MEDIUM,
                        LocalDateTime.now()
                )
        );

        Optional<Investigation> result =
                repository.findByIdAndOrgId(saved.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindInvestigationsByOrganizationAndStatus() {

        repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.ESCALATED,
                        InvestigationPriority.CRITICAL,
                        LocalDateTime.now()
                )
        );

        List<Investigation> results =
                repository.findByOrgIdAndStatusOrderByCreatedAtDesc(
                        1L,
                        InvestigationStatus.ESCALATED
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                i -> i.getOrgId().equals(1L)
                        && i.getStatus() == InvestigationStatus.ESCALATED
        ));
    }

    @Test
    void shouldFindInvestigationsByOrganizationAndPriority() {

        repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.HIGH,
                        LocalDateTime.now()
                )
        );

        List<Investigation> results =
                repository.findByOrgIdAndPriorityOrderByCreatedAtDesc(
                        1L,
                        InvestigationPriority.HIGH
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                i -> i.getOrgId().equals(1L)
                        && i.getPriority() == InvestigationPriority.HIGH
        ));
    }

    @Test
    void shouldFindInvestigationsByOrganizationAndTransactionId() {

        repository.saveAndFlush(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.MEDIUM,
                        LocalDateTime.now()
                )
        );

        List<Investigation> results =
                repository.findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        1L,
                        null
                );

        assertFalse(results.isEmpty());

        assertTrue(results.stream().allMatch(
                i -> i.getOrgId().equals(1L)
                        && i.getTransactionId() == null
        ));
    }

    @Test
    void shouldFindLatestInvestigationByOrganizationAndTransactionId() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.LOW,
                        now.minusHours(2)
                ),
                createInvestigation(
                        InvestigationStatus.INVESTIGATING,
                        InvestigationPriority.HIGH,
                        now
                )
        ));

        Optional<Investigation> result =
                repository.findTopByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        1L,
                        null
                );

        assertTrue(result.isPresent());

        assertEquals(
                InvestigationStatus.INVESTIGATING,
                result.get().getStatus()
        );
    }

    @Test
    void shouldFindLatestInvestigationByOrganizationAndReconciliationId() {

        LocalDateTime now = LocalDateTime.now();

        repository.saveAllAndFlush(List.of(
                createInvestigation(
                        InvestigationStatus.OPEN,
                        InvestigationPriority.LOW,
                        now.minusHours(1)
                ),
                createInvestigation(
                        InvestigationStatus.RESOLVED,
                        InvestigationPriority.CRITICAL,
                        now
                )
        ));

        Optional<Investigation> result =
                repository.findTopByOrgIdAndReconciliationRecordIdOrderByCreatedAtDesc(
                        1L,
                        null
                );

        assertTrue(result.isPresent());

        assertEquals(
                InvestigationStatus.RESOLVED,
                result.get().getStatus()
        );
    }
}