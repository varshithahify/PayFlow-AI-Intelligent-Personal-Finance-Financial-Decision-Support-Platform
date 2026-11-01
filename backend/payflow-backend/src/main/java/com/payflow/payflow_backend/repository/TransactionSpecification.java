package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    private TransactionSpecification() {
        // Utility class
    }

    public static Specification<Transaction> filterTransactions(
            Long orgId,
            Long userId,
            String search,
            TransactionStatus status,
            String paymentMethod) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Tenant isolation: organization
            predicates.add(
                    criteriaBuilder.equal(
                            root.get("orgId"), orgId));

            // Tenant isolation: user
            predicates.add(
                    criteriaBuilder.equal(
                            root.get("userId"), userId));

            // Search by transaction ID, payment method, or currency
            if (search != null && !search.isBlank()) {

                String searchValue = search.trim();

                List<Predicate> searchPredicates = new ArrayList<>();

                /*
                 * Support transaction ID formats:
                 * 20
                 * TXN-20
                 * txn-20
                 * Txn-20
                 */

                String normalizedSearch = searchValue;

                if (normalizedSearch
                        .toUpperCase()
                        .startsWith("TXN-")) {

                    normalizedSearch =
                            normalizedSearch.substring(4).trim();
                }

                // Search by numeric transaction ID
                try {

                    Long transactionId =
                            Long.parseLong(normalizedSearch);

                    searchPredicates.add(
                            criteriaBuilder.equal(
                                    root.get("id"), transactionId));

                } catch (NumberFormatException ignored) {
                    // Non-numeric values are handled by text search.
                }

                // Search by payment method or currency
                String searchPattern =
                        "%" + searchValue.toLowerCase() + "%";

                searchPredicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("paymentMethod")),
                                searchPattern));

                searchPredicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("currency")),
                                searchPattern));

                predicates.add(
                        criteriaBuilder.or(
                                searchPredicates.toArray(
                                        new Predicate[0])));
            }

            // Filter by transaction status
            if (status != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"), status));
            }

            // Filter by payment method
            if (paymentMethod != null
                    && !paymentMethod.isBlank()) {

                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.upper(
                                        root.get("paymentMethod")),
                                paymentMethod.trim().toUpperCase()));
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0]));
        };
    }
}