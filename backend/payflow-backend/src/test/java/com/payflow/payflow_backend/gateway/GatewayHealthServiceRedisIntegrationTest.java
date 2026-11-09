package com.payflow.payflow_backend.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GatewayHealthServiceRedisIntegrationTest {

    @Test
    void shouldConnectToRealRedisAndPerformOperations() {

        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory("localhost", 6380);

        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate =
                new StringRedisTemplate(connectionFactory);

        redisTemplate.afterPropertiesSet();

        String gateway = "redis-test";
        String key = "gateway:health:" + gateway;

        try {
            // Ensure the test starts with a clean key.
            redisTemplate.delete(key);

            GatewayHealthService service =
                    new GatewayHealthService(redisTemplate);

            // Record a successful gateway result.
            service.recordResult(gateway, true, 100);

            // Read actual data stored in Redis.
            Map<Object, Object> data =
                    redisTemplate.opsForHash().entries(key);

            assertEquals("1", data.get("success").toString());
            assertEquals("1", data.get("total").toString());
            assertEquals("100", data.get("total_latency").toString());

            // Verify the health score calculated from Redis data.
            double score = service.getHealthScore(gateway);

            assertEquals(0.98, score, 0.0001);

            // Verify Redis expiration was configured.
            assertTrue(
                    redisTemplate.getExpire(key, TimeUnit.SECONDS) > 0
            );

        } finally {
            // Delete only our test key.
            redisTemplate.delete(key);

            // Close the Redis connection factory.
            connectionFactory.destroy();
        }
    }
}
