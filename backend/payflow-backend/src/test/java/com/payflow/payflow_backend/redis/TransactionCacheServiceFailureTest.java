package com.payflow.payflow_backend.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.service.TransactionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionCacheServiceFailureTest {

    private StringRedisTemplate redisTemplate;
    private TransactionCacheService transactionCacheService;

    @BeforeEach
    void setUp() {

        redisTemplate = mock(StringRedisTemplate.class);

        transactionCacheService =
                new TransactionCacheService(
                        redisTemplate,
                        new ObjectMapper());
    }

    @Test
    void shouldReturnNullWhenRedisReadFails() {

        when(redisTemplate.opsForValue())
                .thenThrow(new RuntimeException(
                        "Redis unavailable"));

        Transaction result =
                transactionCacheService.getCachedTransaction(
                        1L,
                        100L);

        assertNull(result);
    }

    @Test
    void shouldNotThrowWhenRedisWriteFails() {

        Transaction transaction =
                mock(Transaction.class);

        when(redisTemplate.opsForValue())
                .thenThrow(new RuntimeException(
                        "Redis unavailable"));

        assertDoesNotThrow(() ->
                transactionCacheService.cacheTransaction(
                        1L,
                        100L,
                        transaction));
    }

    @Test
    void shouldNotThrowWhenRedisEvictionFails() {

        when(redisTemplate.delete(
                "transaction:1:100"))
                .thenThrow(new RuntimeException(
                        "Redis unavailable"));

        assertDoesNotThrow(() ->
                transactionCacheService.evictTransaction(
                        1L,
                        100L));
    }
}