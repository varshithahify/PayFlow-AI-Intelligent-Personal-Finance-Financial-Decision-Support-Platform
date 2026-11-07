package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.WebhookConfig;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class WebhookConfigRepositoryTest {

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
    private WebhookConfigRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private WebhookConfig createWebhook(
            Long orgId,
            String endpoint,
            boolean active) {

        return new WebhookConfig(
                orgId,
                endpoint,
                "webhook-secret-key",
                "transaction.success,transaction.failed",
                active
        );
    }

    @Test
    void shouldSaveAndRetrieveWebhookConfiguration() {

        WebhookConfig saved = repository.saveAndFlush(
                createWebhook(
                        1L,
                        "https://merchant.example.com/webhooks",
                        true
                )
        );

        Optional<WebhookConfig> result =
                repository.findById(saved.getId());

        assertTrue(result.isPresent());

        WebhookConfig webhook = result.get();

        assertEquals(1L, webhook.getOrgId());

        assertEquals(
                "https://merchant.example.com/webhooks",
                webhook.getEndpointUrl()
        );

        assertEquals("webhook-secret-key", webhook.getSecretKey());

        assertEquals(
                "transaction.success,transaction.failed",
                webhook.getEvents()
        );

        assertTrue(webhook.isActive());
        assertNotNull(webhook.getCreatedAt());
    }

    @Test
    void shouldFindAllWebhooksByOrganizationNewestFirst() {

        WebhookConfig first = repository.saveAndFlush(
                createWebhook(
                        1L,
                        "https://merchant.example.com/first",
                        true
                )
        );

        WebhookConfig second = repository.saveAndFlush(
                createWebhook(
                        1L,
                        "https://merchant.example.com/second",
                        false
                )
        );

        LocalDateTime older =
                LocalDateTime.of(2026, 1, 1, 10, 0);

        LocalDateTime newer =
                LocalDateTime.of(2026, 1, 2, 10, 0);

        jdbcTemplate.update(
                "UPDATE webhook_configs SET created_at = ? WHERE id = ?",
                older,
                first.getId()
        );

        jdbcTemplate.update(
                "UPDATE webhook_configs SET created_at = ? WHERE id = ?",
                newer,
                second.getId()
        );

        entityManager.clear();

        List<WebhookConfig> results =
                repository.findByOrgIdOrderByCreatedAtDesc(1L);

        assertEquals(2, results.size());

        assertEquals(second.getId(), results.get(0).getId());
        assertEquals(first.getId(), results.get(1).getId());
    }

    @Test
    void shouldFindWebhookByOrganizationAndId() {

        WebhookConfig saved = repository.saveAndFlush(
                createWebhook(
                        1L,
                        "https://merchant.example.com/payment-events",
                        true
                )
        );

        Optional<WebhookConfig> result =
                repository.findByOrgIdAndId(
                        1L,
                        saved.getId()
                );

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void shouldNotFindWebhookForDifferentOrganization() {

        WebhookConfig saved = repository.saveAndFlush(
                createWebhook(
                        1L,
                        "https://merchant.example.com/payment-events",
                        true
                )
        );

        Optional<WebhookConfig> result =
                repository.findByOrgIdAndId(
                        999L,
                        saved.getId()
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindOnlyActiveWebhooksByOrganization() {

        repository.saveAllAndFlush(List.of(
                createWebhook(
                        1L,
                        "https://merchant.example.com/active-one",
                        true
                ),
                createWebhook(
                        1L,
                        "https://merchant.example.com/inactive",
                        false
                ),
                createWebhook(
                        1L,
                        "https://merchant.example.com/active-two",
                        true
                )
        ));

        List<WebhookConfig> results =
                repository.findByOrgIdAndActiveTrueOrderByCreatedAtDesc(
                        1L
                );

        assertEquals(2, results.size());

        assertTrue(results.stream().allMatch(
                WebhookConfig::isActive
        ));
    }

    @Test
    void shouldNotReturnActiveWebhooksForDifferentOrganization() {

        repository.saveAndFlush(
                createWebhook(
                        1L,
                        "https://merchant.example.com/active",
                        true
                )
        );

        List<WebhookConfig> results =
                repository.findByOrgIdAndActiveTrueOrderByCreatedAtDesc(
                        999L
                );

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenOrganizationHasNoWebhooks() {

        List<WebhookConfig> results =
                repository.findByOrgIdOrderByCreatedAtDesc(999L);

        assertTrue(results.isEmpty());
    }
}