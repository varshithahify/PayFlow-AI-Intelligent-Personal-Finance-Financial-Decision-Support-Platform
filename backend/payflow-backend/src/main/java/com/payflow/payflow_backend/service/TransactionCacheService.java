package com.payflow.payflow_backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionCacheService {

    private static final String TRANSACTION_KEY_PREFIX =
            "transaction:";

    private final StringRedisTemplate redisTemplate;

    public TransactionCacheService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    public void cacheTransaction(
            Long transactionId,
            String transactionData) {

        String key =
                TRANSACTION_KEY_PREFIX + transactionId;

        redisTemplate.opsForValue().set(
                key,
                transactionData);
    }

    public String getCachedTransaction(
            Long transactionId) {

        String key =
                TRANSACTION_KEY_PREFIX + transactionId;

        return redisTemplate
                .opsForValue()
                .get(key);
    }

    public void evictTransaction(
            Long transactionId) {

        String key =
                TRANSACTION_KEY_PREFIX + transactionId;

        redisTemplate.delete(key);
    }
}