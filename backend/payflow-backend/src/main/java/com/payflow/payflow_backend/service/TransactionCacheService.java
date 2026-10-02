package com.payflow.payflow_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TransactionCacheService {

    private static final Logger logger =
            LoggerFactory.getLogger(TransactionCacheService.class);

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

            logger.warn(
                    "Failed to serialize transaction for cache: transactionId={}",
                    transactionId,
                    exception);

        } catch (RuntimeException exception) {

            logger.warn(
                    "Redis unavailable while caching transaction: transactionId={}",
                    transactionId,
                    exception);
        }
    }

    public Transaction getCachedTransaction(
            Long userId,
            Long transactionId) {

        String key = buildKey(userId, transactionId);

        try {

            String transactionData =
                    redisTemplate
                            .opsForValue()
                            .get(key);

            if (transactionData == null) {
                return null;
            }

            return objectMapper.readValue(
                    transactionData,
                    Transaction.class);

        } catch (JsonProcessingException exception) {

            logger.warn(
                    "Failed to deserialize cached transaction: transactionId={}",
                    transactionId,
                    exception);

            return null;

        } catch (RuntimeException exception) {

            logger.warn(
                    "Redis unavailable while reading transaction cache: transactionId={}",
                    transactionId,
                    exception);

            return null;
        }
    }

    public void evictTransaction(
            Long userId,
            Long transactionId) {

        String key = buildKey(userId, transactionId);

        try {

            redisTemplate.delete(key);

        } catch (RuntimeException exception) {

            logger.warn(
                    "Redis unavailable while evicting transaction cache: transactionId={}",
                    transactionId,
                    exception);
        }
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