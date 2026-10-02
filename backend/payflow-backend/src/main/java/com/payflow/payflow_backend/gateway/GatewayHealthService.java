package com.payflow.payflow_backend.gateway;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GatewayHealthService {

    private static final String HEALTH_KEY_PREFIX =
            "gateway:health:";

    private static final String HEALTHY =
            "UP";

    private static final String UNHEALTHY =
            "DOWN";

    private static final Duration HEALTH_TTL =
            Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public GatewayHealthService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    public void setHealthy(String gateway) {

        String key = buildKey(gateway);

        redisTemplate.opsForValue().set(
                key,
                HEALTHY,
                HEALTH_TTL);
    }

    public void setUnhealthy(String gateway) {

        String key = buildKey(gateway);

        redisTemplate.opsForValue().set(
                key,
                UNHEALTHY,
                HEALTH_TTL);
    }

    public String getHealth(String gateway) {

        String key = buildKey(gateway);

        String health =
                redisTemplate
                        .opsForValue()
                        .get(key);

        if (health == null) {
            return HEALTHY;
        }

        return health;
    }

    public boolean isHealthy(String gateway) {

        return HEALTHY.equals(
                getHealth(gateway));
    }

    public Duration getHealthTtl() {

        return HEALTH_TTL;
    }

    private String buildKey(String gateway) {

        return HEALTH_KEY_PREFIX
                + gateway;
    }
}