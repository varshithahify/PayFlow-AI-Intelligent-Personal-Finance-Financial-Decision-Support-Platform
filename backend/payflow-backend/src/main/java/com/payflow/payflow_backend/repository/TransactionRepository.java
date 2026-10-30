package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

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

    // Find a transaction using its organization, user, and idempotency key.
    Optional<Transaction> findByOrgIdAndUserIdAndIdempotencyKey(
            Long orgId,
            Long userId,
            String idempotencyKey
    );
}

