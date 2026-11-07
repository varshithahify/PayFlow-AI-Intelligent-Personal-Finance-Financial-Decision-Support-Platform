package com.payflow.payflow_backend.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
import com.payflow.payflow_backend.service.TransactionCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionCacheServiceFailureTest {

    private static final Long USER_ID = 1L;
    private static final Long TRANSACTION_ID = 100L;

    private static final String CACHE_KEY =
            "transaction:1:100";

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(10);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;

    private TransactionCacheService transactionCacheService;

    private Transaction transaction;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper().findAndRegisterModules();

        transactionCacheService =
                new TransactionCacheService(
                        redisTemplate,
                        objectMapper
                );

        transaction = new Transaction(
                new BigDecimal("1500.00"),
                "INR",
                "UPI",
                TransactionStatus.values()[0],
                20L,
                10L,
                "idempotency-100"
        );

        transaction.setId(TRANSACTION_ID);
    }

    @Test
    void shouldSuccessfullyCacheTransaction() throws Exception {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        transactionCacheService.cacheTransaction(
                USER_ID,
                TRANSACTION_ID,
                transaction
        );

        verify(valueOperations).set(
                eq(CACHE_KEY),
                anyString(),
                eq(CACHE_TTL)
        );
    }

    @Test
    void shouldUseCorrectCacheKey() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        transactionCacheService.cacheTransaction(
                USER_ID,
                TRANSACTION_ID,
                transaction
        );

        verify(valueOperations).set(
                eq("transaction:1:100"),
                anyString(),
                any(Duration.class)
        );
    }

    @Test
    void shouldSuccessfullyRetrieveCachedTransaction()
            throws Exception {

        String transactionJson =
                objectMapper.writeValueAsString(transaction);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get(CACHE_KEY))
                .thenReturn(transactionJson);

        Transaction result =
                transactionCacheService.getCachedTransaction(
                        USER_ID,
                        TRANSACTION_ID
                );

        assertNotNull(result);
        assertEquals(TRANSACTION_ID, result.getId());
        assertEquals(
                new BigDecimal("1500.00"),
                result.getAmount()
        );
        assertEquals("INR", result.getCurrency());
        assertEquals("UPI", result.getPaymentMethod());

        verify(valueOperations).get(CACHE_KEY);
    }

    @Test
    void shouldReturnNullWhenCacheIsEmpty() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get(CACHE_KEY))
                .thenReturn(null);

        Transaction result =
                transactionCacheService.getCachedTransaction(
                        USER_ID,
                        TRANSACTION_ID
                );

        assertNull(result);
    }

    @Test
    void shouldNotThrowWhenRedisReadFails() {

        when(redisTemplate.opsForValue())
                .thenThrow(new RuntimeException(
                        "Redis unavailable"
                ));

        assertDoesNotThrow(() -> {

            Transaction result =
                    transactionCacheService.getCachedTransaction(
                            USER_ID,
                            TRANSACTION_ID
                    );

            assertNull(result);
        });
    }

    @Test
    void shouldNotThrowWhenRedisWriteFails() {

        when(redisTemplate.opsForValue())
                .thenThrow(new RuntimeException(
                        "Redis unavailable"
                ));

        assertDoesNotThrow(() ->
                transactionCacheService.cacheTransaction(
                        USER_ID,
                        TRANSACTION_ID,
                        transaction
                ));
    }

    @Test
    void shouldNotThrowWhenRedisEvictionFails() {

        when(redisTemplate.delete(CACHE_KEY))
                .thenThrow(new RuntimeException(
                        "Redis unavailable"
                ));

        assertDoesNotThrow(() ->
                transactionCacheService.evictTransaction(
                        USER_ID,
                        TRANSACTION_ID
                ));
    }

    @Test
    void shouldSuccessfullyEvictTransaction() {

        when(redisTemplate.delete(CACHE_KEY))
                .thenReturn(true);

        assertDoesNotThrow(() ->
                transactionCacheService.evictTransaction(
                        USER_ID,
                        TRANSACTION_ID
                ));

        verify(redisTemplate).delete(CACHE_KEY);
    }

    @Test
    void shouldReturnNullWhenCachedJsonIsInvalid() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get(CACHE_KEY))
                .thenReturn("{invalid-json");

        Transaction result =
                transactionCacheService.getCachedTransaction(
                        USER_ID,
                        TRANSACTION_ID
                );

        assertNull(result);
    }

    @Test
    void shouldNotThrowWhenTransactionSerializationFails()
            throws Exception {

        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);

        TransactionCacheService service =
                new TransactionCacheService(
                        redisTemplate,
                        failingObjectMapper
                );

        when(failingObjectMapper.writeValueAsString(transaction))
                .thenThrow(new JsonProcessingException(
                        "Serialization failed"
                ) {});

        assertDoesNotThrow(() ->
                service.cacheTransaction(
                        USER_ID,
                        TRANSACTION_ID,
                        transaction
                ));

        verifyNoInteractions(redisTemplate);
    }
}