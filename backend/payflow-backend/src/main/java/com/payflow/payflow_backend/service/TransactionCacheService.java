package com.payflow.payflow_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TransactionCacheService {

    private static final String TRANSACTION_KEY_PREFIX =
            "transaction:";

    private static final Duration CACHE_TTL =
            Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TransactionCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {

        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheTransaction(
            Long userId,
            Long transactionId,
            Transaction transaction) {

        String key = buildKey(userId, transactionId);

        try {

            String transactionData =
                    objectMapper.writeValueAsString(transaction);

            redisTemplate.opsForValue().set(
                    key,
                    transactionData,
                    CACHE_TTL);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to serialize transaction for cache",
                    exception);
        }
    }

    public Transaction getCachedTransaction(
            Long userId,
            Long transactionId) {

        String key = buildKey(userId, transactionId);

        String transactionData =
                redisTemplate
                        .opsForValue()
                        .get(key);

        if (transactionData == null) {
            return null;
        }

        try {

            return objectMapper.readValue(
                    transactionData,
                    Transaction.class);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to deserialize transaction from cache",
                    exception);
        }
    }

    public void evictTransaction(
            Long userId,
            Long transactionId) {

        String key = buildKey(userId, transactionId);

        redisTemplate.delete(key);
    }

    private String buildKey(
            Long userId,
            Long transactionId) {

        return TRANSACTION_KEY_PREFIX
                + userId
                + ":"
                + transactionId;
    }
}