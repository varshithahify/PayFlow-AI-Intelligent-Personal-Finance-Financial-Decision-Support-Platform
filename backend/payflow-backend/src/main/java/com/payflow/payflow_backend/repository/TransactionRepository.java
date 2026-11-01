package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>,
                JpaSpecificationExecutor<Transaction> {

    // Retrieve a transaction only when its ID and organization match.
    Optional<Transaction> findByIdAndOrgId(
            Long id,
            Long orgId
    );

    // Retrieve transactions belonging to a specific organization and user.
    List<Transaction> findByOrgIdAndUserIdOrderByCreatedAtDesc(
            Long orgId,
            Long userId
    );

    // Retrieve paginated transactions belonging to a specific organization and user.
    Page<Transaction> findByOrgIdAndUserId(
            Long orgId,
            Long userId,
            Pageable pageable
    );

    // Find a transaction using its organization, user, and idempotency key.
    Optional<Transaction> findByOrgIdAndUserIdAndIdempotencyKey(
            Long orgId,
            Long userId,
            String idempotencyKey
    );

    // Analytics: Count all transactions belonging to an organization.
    long countByOrgId(Long orgId);

    // Analytics: Count transactions by organization and status.
    long countByOrgIdAndStatus(
            Long orgId,
            TransactionStatus status
    );

    // Analytics: Calculate payment volume grouped by currency.
    @Query("""
            SELECT t.currency, SUM(t.amount)
            FROM Transaction t
            WHERE t.orgId = :orgId
              AND t.status = :status
            GROUP BY t.currency
            """)
    List<Object[]> findPaymentVolumeByCurrency(
            @Param("orgId") Long orgId,
            @Param("status") TransactionStatus status
    );

    // Analytics: Daily transaction count grouped by date.
    @Query("""
            SELECT FUNCTION('DATE', t.createdAt), COUNT(t)
            FROM Transaction t
            WHERE t.orgId = :orgId
              AND t.createdAt >= :startDate
              AND t.createdAt < :endDate
            GROUP BY FUNCTION('DATE', t.createdAt)
            ORDER BY FUNCTION('DATE', t.createdAt)
            """)
    List<Object[]> findDailyTransactionCounts(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Analytics: Transaction count grouped by payment gateway.
    @Query("""
            SELECT t.gatewayName, COUNT(t)
            FROM Transaction t
            WHERE t.orgId = :orgId
            GROUP BY t.gatewayName
            """)
    List<Object[]> findTransactionCountsByGateway(
            @Param("orgId") Long orgId
    );

    // Analytics: Transaction counts grouped by gateway and status.
    @Query("""
            SELECT t.gatewayName, t.status, COUNT(t)
            FROM Transaction t
            WHERE t.orgId = :orgId
              AND t.gatewayName IS NOT NULL
            GROUP BY t.gatewayName, t.status
            ORDER BY t.gatewayName
            """)
    List<Object[]> findTransactionCountsByGatewayAndStatus(
            @Param("orgId") Long orgId
    );

    // Analytics: Successful payment volume grouped by gateway and currency.
    @Query("""
            SELECT t.gatewayName, t.currency, SUM(t.amount)
            FROM Transaction t
            WHERE t.orgId = :orgId
              AND t.gatewayName IS NOT NULL
              AND t.status = :status
            GROUP BY t.gatewayName, t.currency
            ORDER BY t.gatewayName, t.currency
            """)
    List<Object[]> findSuccessfulPaymentVolumeByGatewayAndCurrency(
            @Param("orgId") Long orgId,
            @Param("status") TransactionStatus status
    );

    // Analytics: Transaction count grouped by payment method.
    @Query("""
            SELECT t.paymentMethod, COUNT(t)
            FROM Transaction t
            WHERE t.orgId = :orgId
            GROUP BY t.paymentMethod
            """)
    List<Object[]> findTransactionCountsByPaymentMethod(
            @Param("orgId") Long orgId
    );
}

