package com.payflow.payflow_backend.repository;

import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSpecificationTest {

    @Mock
    private Root<Transaction> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate predicate;

    @Mock
    private Path<Long> longPath;

    @Mock
    private Path<String> stringPath;

    @BeforeEach
    void setUp() {

        // Shared stubs are lenient because different tests
        // exercise different Criteria API branches.

        lenient().when(criteriaBuilder.and(any(Predicate[].class)))
            .thenReturn(predicate);

        lenient().when(criteriaBuilder.or(any(Predicate[].class)))
            .thenReturn(predicate);

        lenient().when(criteriaBuilder.equal(
            any(Expression.class),
            any(Object.class)
        )).thenReturn(predicate);

        lenient().when(criteriaBuilder.like(
            any(Expression.class),
            anyString()
        )).thenReturn(predicate);

        lenient().when(criteriaBuilder.lower(
            any(Expression.class)
        )).thenReturn(stringPath);

        lenient().when(criteriaBuilder.upper(
            any(Expression.class)
        )).thenReturn(stringPath);

        lenient().when(root.<Long>get(anyString()))
            .thenReturn(longPath);

        lenient().when(root.<String>get(anyString()))
            .thenReturn(stringPath);
    }

    // --------------------------------------------------
    // 1. Organization and user isolation
    // --------------------------------------------------

    @Test
    void shouldAlwaysApplyOrganizationAndUserFilters() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, null, null, null
            );

        Predicate result = specification.toPredicate(
            root, query, criteriaBuilder
        );

        assertSame(predicate, result);

        verify(root).get("orgId");
        verify(root).get("userId");

        verify(criteriaBuilder, times(2)).equal(
            any(Expression.class),
            any(Object.class)
        );

        verify(criteriaBuilder).and(any(Predicate[].class));
    }

    // --------------------------------------------------
    // 2. Numeric transaction ID search
    // --------------------------------------------------

    @Test
    void shouldSearchUsingNumericTransactionId() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "20", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("id");

        verify(criteriaBuilder, times(3)).equal(
            any(Expression.class),
            any(Object.class)
        );

        verify(criteriaBuilder).or(any(Predicate[].class));
    }

    // --------------------------------------------------
    // 3. TXN-prefixed transaction ID search
    // --------------------------------------------------

    @Test
    void shouldSearchUsingTxnPrefixedTransactionId() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "TXN-20", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("id");

        verify(criteriaBuilder, times(3)).equal(
            any(Expression.class),
            any(Object.class)
        );
    }

    // --------------------------------------------------
    // 4. Lowercase TXN prefix
    // --------------------------------------------------

    @Test
    void shouldSearchUsingLowercaseTxnPrefix() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "txn-20", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("id");

        verify(criteriaBuilder).or(any(Predicate[].class));
    }

    // --------------------------------------------------
    // 5. Search with whitespace
    // --------------------------------------------------

    @Test
    void shouldTrimWhitespaceFromSearchValue() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "  TXN-20  ", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("id");

        verify(criteriaBuilder).or(any(Predicate[].class));
    }

    // --------------------------------------------------
    // 6. Non-numeric text search
    // --------------------------------------------------

    @Test
    void shouldHandleNonNumericSearchValue() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "UPI", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root, never()).get("id");

        verify(root).get("paymentMethod");
        verify(root).get("currency");

        // lower() is called for both paymentMethod and currency.
        verify(criteriaBuilder, times(2)).lower(stringPath);

        verify(criteriaBuilder, times(2)).like(
            eq(stringPath),
            eq("%upi%")
        );

        verify(criteriaBuilder).or(any(Predicate[].class));
    }

    // --------------------------------------------------
    // 7. Currency search
    // --------------------------------------------------

    @Test
    void shouldSearchByCurrency() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "INR", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("currency");

        // Search checks both currency and payment method.
        verify(criteriaBuilder, times(2)).like(
            eq(stringPath),
            eq("%inr%")
        );
    }

    // --------------------------------------------------
    // 8. Payment method search
    // --------------------------------------------------

    @Test
    void shouldSearchByPaymentMethod() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "CARD", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("paymentMethod");

        verify(criteriaBuilder, times(2)).like(
            eq(stringPath),
            eq("%card%")
        );
    }

    // --------------------------------------------------
    // 9. Status filtering
    // --------------------------------------------------

    @Test
    void shouldApplyTransactionStatusFilter() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L,
                10L,
                null,
                TransactionStatus.SUCCESS,
                null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("status");

        verify(criteriaBuilder, times(3)).equal(
            any(Expression.class),
            any(Object.class)
        );
    }

    // --------------------------------------------------
    // 10. Payment method filtering
    // --------------------------------------------------

    @Test
    void shouldApplyPaymentMethodFilter() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, null, null, "upi"
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).get("paymentMethod");

        verify(criteriaBuilder).upper(stringPath);

        verify(criteriaBuilder).equal(
            stringPath,
            "UPI"
        );
    }

    // --------------------------------------------------
    // 11. Ignore blank search
    // --------------------------------------------------

    @Test
    void shouldIgnoreBlankSearchValue() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, "   ", null, null
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root, never()).get("id");
        verify(root, never()).get("paymentMethod");
        verify(root, never()).get("currency");

        verify(criteriaBuilder, times(2)).equal(
            any(Expression.class),
            any(Object.class)
        );
    }

    // --------------------------------------------------
    // 12. Ignore blank payment method
    // --------------------------------------------------

    @Test
    void shouldIgnoreBlankPaymentMethodFilter() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L, 10L, null, null, "   "
            );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root, never()).get("paymentMethod");

        verify(criteriaBuilder, times(2)).equal(
            any(Expression.class),
            any(Object.class)
        );
    }

    // --------------------------------------------------
    // 13. Combined filters
    // --------------------------------------------------

    @Test
    void shouldApplySearchStatusAndPaymentMethodFiltersTogether() {

        var specification =
            TransactionSpecification.filterTransactions(
                1L,
                10L,
                "TXN-20",
                TransactionStatus.SUCCESS,
                "CARD"
            );

        Predicate result = specification.toPredicate(
            root, query, criteriaBuilder
        );

        assertSame(predicate, result);

        verify(root).get("orgId");
        verify(root).get("userId");
        verify(root).get("id");
        verify(root).get("status");

        // Called once for search and once for payment method filter.
        verify(root, times(2)).get("paymentMethod");

        verify(root).get("currency");

        verify(criteriaBuilder).or(any(Predicate[].class));
        verify(criteriaBuilder).and(any(Predicate[].class));
    }
}