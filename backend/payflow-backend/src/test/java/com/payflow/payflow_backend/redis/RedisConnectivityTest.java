package com.payflow.payflow_backend.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RedisConnectivityTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldSetAndGetValueFromRedis() {

        String key = "payflow:test";
        String value = "redis-connected";

        redisTemplate.opsForValue().set(key, value);

        String result =
                redisTemplate.opsForValue().get(key);

        assertEquals(value, result);
    }
}