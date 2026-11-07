package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.AIInvestigationFinding;
import com.payflow.payflow_backend.entity.Investigation;
import com.payflow.payflow_backend.entity.InvestigationIssueType;
import com.payflow.payflow_backend.entity.InvestigationPriority;
import com.payflow.payflow_backend.entity.InvestigationStatus;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;

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
class AIInvestigationFindingRepositoryTest {

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
    private AIInvestigationFindingRepository repository;

    @Autowired
    private InvestigationRepository investigationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long createUser() {

        User user = new User(
                "AI Finding Test User",
                "ai-finding-test@example.com",
                "encoded-password",
                1L
        );

        return userRepository.saveAndFlush(user).getId();
    }

    private Transaction createTransaction(Long userId) {

        Transaction transaction = new Transaction(
                new BigDecimal("1500.00"),
                "INR",
                "UPI",
                TransactionStatus.SUCCESS,
                userId,
                1L,
                "AI-FINDING-IDEM-" + System.nanoTime()
        );

        return transactionRepository.saveAndFlush(transaction);
    }

    private Investigation createInvestigation(Long transactionId) {

        Investigation investigation = new Investigation();

        investigation.setTransactionId(transactionId);
        investigation.setOrgId(1L);
        investigation.setStatus(InvestigationStatus.OPEN);
        investigation.setPriority(InvestigationPriority.HIGH);
        investigation.setIssueType(
                InvestigationIssueType.AMOUNT_MISMATCH
        );
        investigation.setSummary("Payment amount mismatch investigation");

        LocalDateTime now = LocalDateTime.now();

        investigation.setCreatedAt(now);
        investigation.setUpdatedAt(now);

        return investigationRepository.saveAndFlush(investigation);
    }

    private AIInvestigationFinding createFinding(
            Long orgId,
            Long investigationId,
            Long transactionId,
            String likelyCause,
            boolean requiresHumanReview) {

        AIInvestigationFinding finding =
                new AIInvestigationFinding();

        finding.setOrgId(orgId);
        finding.setInvestigationId(investigationId);
        finding.setTransactionId(transactionId);
        finding.setLikelyCause(likelyCause);
        finding.setEvidence("Payment records indicate an amount mismatch.");
        finding.setRecommendedActions("Verify the payment gateway records.");
        finding.setLimitations("External gateway data may be delayed.");
        finding.setRequiresHumanReview(requiresHumanReview);
        finding.setModelName("llama3.2:3b");

        return finding;
    }

    private AIInvestigationFinding saveFinding(
            Long investigationId,
            Long transactionId,
            String likelyCause) {

        return repository.saveAndFlush(
                createFinding(
                        1L,
                        investigationId,
                        transactionId,
                        likelyCause,
                        true
                )
        );
    }

    @Test
    void shouldSaveAndRetrieveAIInvestigationFinding() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        AIInvestigationFinding saved = saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Possible payment gateway inconsistency"
        );

        Optional<AIInvestigationFinding> result =
                repository.findById(saved.getId());

        assertTrue(result.isPresent());

        AIInvestigationFinding finding = result.get();

        assertEquals(1L, finding.getOrgId());
        assertEquals(investigation.getId(), finding.getInvestigationId());
        assertEquals(transaction.getId(), finding.getTransactionId());

        assertEquals(
                "Possible payment gateway inconsistency",
                finding.getLikelyCause()
        );

        assertEquals("llama3.2:3b", finding.getModelName());
        assertTrue(finding.isRequiresHumanReview());
        assertNotNull(finding.getCreatedAt());
    }

    @Test
    void shouldFindFindingsByOrganizationAndInvestigation() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        saveFinding(
                investigation.getId(),
                transaction.getId(),
                "First possible cause"
        );

        saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Second possible cause"
        );

        List<AIInvestigationFinding> results =
                repository.findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        1L,
                        investigation.getId()
                );

        assertEquals(2, results.size());

        assertTrue(results.stream().allMatch(
                finding ->
                        finding.getOrgId().equals(1L)
                                && finding.getInvestigationId()
                                .equals(investigation.getId())
        ));
    }

    @Test
    void shouldReturnFindingsInNewestFirstOrder() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        AIInvestigationFinding first = saveFinding(
                investigation.getId(),
                transaction.getId(),
                "First finding"
        );

        AIInvestigationFinding second = saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Second finding"
        );

        LocalDateTime olderTime =
                LocalDateTime.of(2026, 1, 1, 10, 0);

        LocalDateTime newerTime =
                LocalDateTime.of(2026, 1, 2, 10, 0);

        jdbcTemplate.update(
                "UPDATE ai_investigation_findings SET created_at = ? WHERE id = ?",
                olderTime,
                first.getId()
        );

        jdbcTemplate.update(
                "UPDATE ai_investigation_findings SET created_at = ? WHERE id = ?",
                newerTime,
                second.getId()
        );

        entityManager.clear();

        List<AIInvestigationFinding> results =
                repository.findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        1L,
                        investigation.getId()
                );

        assertEquals(2, results.size());

        assertEquals(second.getId(), results.get(0).getId());
        assertEquals(first.getId(), results.get(1).getId());
    }

    @Test
    void shouldFindLatestFindingByOrganizationAndInvestigation() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        AIInvestigationFinding first = saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Older finding"
        );

        AIInvestigationFinding latest = saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Latest finding"
        );

        jdbcTemplate.update(
                "UPDATE ai_investigation_findings SET created_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                first.getId()
        );

        jdbcTemplate.update(
                "UPDATE ai_investigation_findings SET created_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 1, 2, 10, 0),
                latest.getId()
        );

        entityManager.clear();

        Optional<AIInvestigationFinding> result =
                repository.findTopByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        1L,
                        investigation.getId()
                );

        assertTrue(result.isPresent());
        assertEquals(latest.getId(), result.get().getId());
    }

    @Test
    void shouldFindFindingsByOrganizationAndTransaction() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        saveFinding(
                investigation.getId(),
                transaction.getId(),
                "First transaction finding"
        );

        saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Second transaction finding"
        );

        List<AIInvestigationFinding> results =
                repository.findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        1L,
                        transaction.getId()
                );

        assertEquals(2, results.size());

        assertTrue(results.stream().allMatch(
                finding ->
                        finding.getOrgId().equals(1L)
                                && finding.getTransactionId()
                                .equals(transaction.getId())
        ));
    }

    @Test
    void shouldNotReturnFindingsForDifferentOrganization() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        saveFinding(
                investigation.getId(),
                transaction.getId(),
                "Organization one finding"
        );

        List<AIInvestigationFinding> investigationResults =
                repository.findByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        999L,
                        investigation.getId()
                );

        List<AIInvestigationFinding> transactionResults =
                repository.findByOrgIdAndTransactionIdOrderByCreatedAtDesc(
                        999L,
                        transaction.getId()
                );

        Optional<AIInvestigationFinding> latestResult =
                repository.findTopByOrgIdAndInvestigationIdOrderByCreatedAtDesc(
                        999L,
                        investigation.getId()
                );

        assertTrue(investigationResults.isEmpty());
        assertTrue(transactionResults.isEmpty());
        assertTrue(latestResult.isEmpty());
    }

    @Test
    void shouldUseDefaultHumanReviewFlagAndCreationTimestamp() {

        Long userId = createUser();

        Transaction transaction = createTransaction(userId);

        Investigation investigation =
                createInvestigation(transaction.getId());

        AIInvestigationFinding finding =
                new AIInvestigationFinding();

        finding.setOrgId(1L);
        finding.setInvestigationId(investigation.getId());
        finding.setTransactionId(transaction.getId());
        finding.setLikelyCause("Possible processing failure");
        finding.setEvidence("Gateway response was delayed.");
        finding.setRecommendedActions("Review gateway logs.");
        finding.setLimitations("Limited external gateway information.");
        finding.setModelName("llama3.2:3b");

        AIInvestigationFinding saved =
                repository.saveAndFlush(finding);

        assertTrue(saved.isRequiresHumanReview());
        assertNotNull(saved.getCreatedAt());
    }
}