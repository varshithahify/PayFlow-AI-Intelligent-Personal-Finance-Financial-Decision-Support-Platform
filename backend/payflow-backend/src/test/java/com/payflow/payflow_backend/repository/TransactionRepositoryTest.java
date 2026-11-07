package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.entity.User;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class TransactionRepositoryTest {

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
    private TransactionRepository repository;

    @Autowired
    private UserRepository userRepository;

    private Long createUser() {

        User user = new User(
                "Transaction Test User",
                "transaction-test@example.com",
                "encoded-password",
                1L
        );

        return userRepository.saveAndFlush(user).getId();
    }

    private Transaction createTransaction(
            Long userId,
            Long orgId,
            String amount,
            String currency,
            String paymentMethod,
            TransactionStatus status,
            String idempotencyKey,
            String gateway) {

        Transaction transaction = new Transaction(
                new BigDecimal(amount),
                currency,
                paymentMethod,
                status,
                userId,
                orgId,
                idempotencyKey
        );

        transaction.setGatewayName(gateway);

        return transaction;
    }

    @Test
    void shouldFindTransactionByIdAndOrganization() {

        Long userId = createUser();

        Transaction saved = repository.saveAndFlush(
                createTransaction(
                        userId, 1L, "1000.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-001", "RAZORPAY"
                )
        );

        var result = repository.findByIdAndOrgId(
                saved.getId(), 1L
        );

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void shouldNotFindTransactionForDifferentOrganization() {

        Long userId = createUser();

        Transaction saved = repository.saveAndFlush(
                createTransaction(
                        userId, 1L, "1000.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-002", "RAZORPAY"
                )
        );

        var result = repository.findByIdAndOrgId(
                saved.getId(), 999L
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindTransactionsByOrganizationAndUser() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-003", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.FAILED,
                        "IDEM-004", "PHONEPE"
                )
        ));

        List<Transaction> results =
                repository.findByOrgIdAndUserIdOrderByCreatedAtDesc(
                        1L, userId
                );

        assertEquals(2, results.size());

        assertTrue(results.stream().allMatch(
                t -> t.getOrgId().equals(1L)
                        && t.getUserId().equals(userId)
        ));
    }

    @Test
    void shouldReturnPaginatedTransactions() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-005", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.SUCCESS,
                        "IDEM-006", "PHONEPE"
                ),
                createTransaction(
                        userId, 1L, "300.00", "INR",
                        "CARD", TransactionStatus.FAILED,
                        "IDEM-007", "RAZORPAY"
                )
        ));

        Page<Transaction> page = repository.findByOrgIdAndUserId(
                1L,
                userId,
                PageRequest.of(0, 2)
        );

        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void shouldFindTransactionByIdempotencyKey() {

        Long userId = createUser();

        repository.saveAndFlush(
                createTransaction(
                        userId, 1L, "500.00", "INR",
                        "UPI", TransactionStatus.SUCCESS,
                        "IDEM-008", "PHONEPE"
                )
        );

        var result =
                repository.findByOrgIdAndUserIdAndIdempotencyKey(
                        1L, userId, "IDEM-008"
                );

        assertTrue(result.isPresent());
        assertEquals(
                "IDEM-008",
                result.get().getIdempotencyKey()
        );
    }

    @Test
    void shouldCountTransactionsByOrganization() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-009", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.FAILED,
                        "IDEM-010", "PHONEPE"
                )
        ));

        assertEquals(2, repository.countByOrgId(1L));
        assertEquals(0, repository.countByOrgId(999L));
    }

    @Test
    void shouldCountTransactionsByOrganizationAndStatus() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-011", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.SUCCESS,
                        "IDEM-012", "PHONEPE"
                ),
                createTransaction(
                        userId, 1L, "300.00", "INR",
                        "CARD", TransactionStatus.FAILED,
                        "IDEM-013", "RAZORPAY"
                )
        ));

        assertEquals(
                2,
                repository.countByOrgIdAndStatus(
                        1L, TransactionStatus.SUCCESS
                )
        );

        assertEquals(
                1,
                repository.countByOrgIdAndStatus(
                        1L, TransactionStatus.FAILED
                )
        );
    }

    @Test
    void shouldCalculatePaymentVolumeByCurrency() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-014", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.SUCCESS,
                        "IDEM-015", "PHONEPE"
                ),
                createTransaction(
                        userId, 1L, "300.00", "USD",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-016", "STRIPE"
                ),
                createTransaction(
                        userId, 1L, "400.00", "INR",
                        "CARD", TransactionStatus.FAILED,
                        "IDEM-017", "RAZORPAY"
                )
        ));

        List<Object[]> results =
                repository.findPaymentVolumeByCurrency(
                        1L, TransactionStatus.SUCCESS
                );

        Map<String, BigDecimal> volumes =
                results.stream().collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));

        assertEquals(2, volumes.size());
        assertEquals(0, new BigDecimal("300.00")
                .compareTo(volumes.get("INR")));
        assertEquals(0, new BigDecimal("300.00")
                .compareTo(volumes.get("USD")));
    }

    @Test
    void shouldCalculateDailyTransactionCounts() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-018", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.FAILED,
                        "IDEM-019", "PHONEPE"
                )
        ));

        List<Object[]> results =
                repository.findDailyTransactionCounts(
                        1L,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(1)
                );

        assertEquals(1, results.size());
        assertEquals(2L, ((Number) results.get(0)[1]).longValue());
    }

    @Test
    void shouldCalculateTransactionCountsByGateway() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-020", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.FAILED,
                        "IDEM-021", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "300.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-022", "PHONEPE"
                )
        ));

        List<Object[]> results =
                repository.findTransactionCountsByGateway(1L);

        Map<String, Long> counts =
                results.stream().collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        assertEquals(2, counts.get("RAZORPAY"));
        assertEquals(1, counts.get("PHONEPE"));
    }

    @Test
    void shouldCalculateTransactionCountsByGatewayAndStatus() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-023", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.FAILED,
                        "IDEM-024", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "300.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-025", "PHONEPE"
                )
        ));

        List<Object[]> results =
                repository.findTransactionCountsByGatewayAndStatus(1L);

        assertEquals(3, results.size());

        assertTrue(results.stream().anyMatch(
                row -> row[0].equals("RAZORPAY")
                        && row[1] == TransactionStatus.SUCCESS
                        && ((Number) row[2]).longValue() == 1
        ));

        assertTrue(results.stream().anyMatch(
                row -> row[0].equals("RAZORPAY")
                        && row[1] == TransactionStatus.FAILED
                        && ((Number) row[2]).longValue() == 1
        ));
    }

    @Test
    void shouldCalculateSuccessfulPaymentVolumeByGatewayAndCurrency() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-026", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "UPI", TransactionStatus.SUCCESS,
                        "IDEM-027", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "300.00", "USD",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-028", "STRIPE"
                ),
                createTransaction(
                        userId, 1L, "400.00", "INR",
                        "CARD", TransactionStatus.FAILED,
                        "IDEM-029", "RAZORPAY"
                )
        ));

        List<Object[]> results =
                repository.findSuccessfulPaymentVolumeByGatewayAndCurrency(
                        1L, TransactionStatus.SUCCESS
                );

        assertEquals(2, results.size());

        assertTrue(results.stream().anyMatch(
                row -> row[0].equals("RAZORPAY")
                        && row[1].equals("INR")
                        && new BigDecimal("300.00")
                        .compareTo((BigDecimal) row[2]) == 0
        ));

        assertTrue(results.stream().anyMatch(
                row -> row[0].equals("STRIPE")
                        && row[1].equals("USD")
                        && new BigDecimal("300.00")
                        .compareTo((BigDecimal) row[2]) == 0
        ));
    }

    @Test
    void shouldCalculateTransactionCountsByPaymentMethod() {

        Long userId = createUser();

        repository.saveAllAndFlush(List.of(
                createTransaction(
                        userId, 1L, "100.00", "INR",
                        "CARD", TransactionStatus.SUCCESS,
                        "IDEM-030", "RAZORPAY"
                ),
                createTransaction(
                        userId, 1L, "200.00", "INR",
                        "CARD", TransactionStatus.FAILED,
                        "IDEM-031", "PHONEPE"
                ),
                createTransaction(
                        userId, 1L, "300.00", "INR",
                        "UPI", TransactionStatus.SUCCESS,
                        "IDEM-032", "PHONEPE"
                )
        ));

        List<Object[]> results =
                repository.findTransactionCountsByPaymentMethod(1L);

        Map<String, Long> counts =
                results.stream().collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        assertEquals(2, counts.get("CARD"));
        assertEquals(1, counts.get("UPI"));
    }
}