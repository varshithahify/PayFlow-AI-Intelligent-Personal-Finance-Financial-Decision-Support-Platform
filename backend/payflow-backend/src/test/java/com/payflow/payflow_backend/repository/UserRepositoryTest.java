package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Role;
import com.payflow.payflow_backend.entity.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UserRepositoryTest {

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
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveUserByEmail() {

        User user = new User(
                "Test User",
                "test@example.com",
                "encoded-password",
                1L
        );

        userRepository.saveAndFlush(user);

        User result = userRepository.findByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void shouldGenerateUserIdWhenSaved() {

        User user = new User(
                "Generated ID User",
                "generated@example.com",
                "encoded-password",
                1L
        );

        User savedUser = userRepository.saveAndFlush(user);

        assertNotNull(savedUser.getId());
        assertTrue(savedUser.getId() > 0);
    }

    @Test
    void shouldFindAllUsersByOrganizationId() {

        userRepository.saveAndFlush(
                new User("User One", "one@example.com",
                        "password", 1L)
        );

        userRepository.saveAndFlush(
                new User("User Two", "two@example.com",
                        "password", 1L)
        );

        List<User> users = userRepository.findAllByOrgId(1L);

        assertTrue(users.size() >= 2);
        assertTrue(users.stream()
                .allMatch(user -> user.getOrgId().equals(1L)));
    }

    @Test
    void shouldFindUserByEmailAndOrganizationId() {

        userRepository.saveAndFlush(
                new User("Tenant User", "tenant@example.com",
                        "password", 1L)
        );

        Optional<User> result =
                userRepository.findByEmailAndOrgId(
                        "tenant@example.com", 1L
                );

        assertTrue(result.isPresent());
        assertEquals("tenant@example.com",
                result.get().getEmail());
    }

    @Test
    void shouldNotFindUserWhenOrganizationIdDoesNotMatch() {

        userRepository.saveAndFlush(
                new User("Tenant User", "tenant2@example.com",
                        "password", 1L)
        );

        Optional<User> result =
                userRepository.findByEmailAndOrgId(
                        "tenant2@example.com", 999L
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForUnknownOrganization() {

        List<User> users =
                userRepository.findAllByOrgId(999L);

        assertTrue(users.isEmpty());
    }

    @Test
    void shouldPersistUserRoleAndCreatedAt() {

        User user = new User(
                "Admin User",
                "admin@example.com",
                "password",
                1L,
                Role.MERCHANT_ADMIN
        );

        User savedUser = userRepository.saveAndFlush(user);

        User retrievedUser =
                userRepository.findByEmail("admin@example.com");

        assertNotNull(retrievedUser);
        assertEquals(Role.MERCHANT_ADMIN, retrievedUser.getRole());
        assertNotNull(retrievedUser.getCreatedAt());
    }
}