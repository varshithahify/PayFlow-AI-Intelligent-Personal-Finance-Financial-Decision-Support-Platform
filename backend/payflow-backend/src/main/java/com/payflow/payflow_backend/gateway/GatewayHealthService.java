package com.payflow.payflow_backend.gateway;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class GatewayHealthService {

    private static final String HEALTH_KEY_PREFIX =
            "gateway:health:";

    private static final Duration HEALTH_TTL =
            Duration.ofMinutes(10);

    private static final String SUCCESS_FIELD =
            "success";

    private static final String FAILURE_FIELD =
            "failure";

    private static final String TOTAL_FIELD =
            "total";

    private static final String TOTAL_LATENCY_FIELD =
            "total_latency";

    private final StringRedisTemplate redisTemplate;

    public GatewayHealthService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    /**
     * Records the result of a gateway payment attempt.
     *
     * Redis stores:
     * success       -> number of successful requests
     * failure       -> number of failed requests
     * total         -> total number of requests
     * total_latency -> accumulated latency in milliseconds
     */
    public void recordResult(
            String gateway,
            boolean success,
            int latencyMs) {

        String key = buildKey(gateway);

        redisTemplate.opsForHash().increment(
                key,
                success
                        ? SUCCESS_FIELD
                        : FAILURE_FIELD,
                1);

        redisTemplate.opsForHash().increment(
                key,
                TOTAL_FIELD,
                1);

        redisTemplate.opsForHash().increment(
                key,
                TOTAL_LATENCY_FIELD,
                latencyMs);

        redisTemplate.expire(
                key,
                HEALTH_TTL);
    }

    /**
     * Calculates the gateway health score.
     *
     * Score =
     *     success rate * 0.6
     *     +
     *     latency score * 0.4
     *
     * Unknown gateways receive a default score of 1.0.
     */
    public double getHealthScore(String gateway) {

        String key = buildKey(gateway);

        Map<Object, Object> data =
                redisTemplate.opsForHash().entries(key);

        if (data.isEmpty()) {
            return 1.0;
        }

        long success =
                getLong(data, SUCCESS_FIELD);

        long total =
                getLong(data, TOTAL_FIELD);

        long totalLatency =
                getLong(data, TOTAL_LATENCY_FIELD);

        double successRate =
                total > 0
                        ? (double) success / total
                        : 1.0;

        double averageLatency =
                total > 0
                        ? (double) totalLatency / total
                        : 200.0;

        double latencyScore =
                Math.max(
                        0,
                        1 - (averageLatency / 2000));

        return (successRate * 0.6)
                + (latencyScore * 0.4);
    }

    /**
     * Converts a Redis hash value into a long.
     */
    private long getLong(
            Map<Object, Object> data,
            String field) {

        Object value = data.get(field);

        if (value == null) {
            return 0;
        }

        return Long.parseLong(value.toString());
    }

    /**
     * Compatibility method.
     *
     * The new health model is score-based, so an explicit
     * UP/DOWN string is no longer stored in Redis.
     */
    public void setHealthy(String gateway) {
        // Health is now calculated from recorded gateway results.
    }

    /**
     * Compatibility method.
     *
     * The new health model is score-based, so an explicit
     * UP/DOWN string is no longer stored in Redis.
     */
    public void setUnhealthy(String gateway) {
        // Health is now calculated from recorded gateway results.
    }

    /**
     * Returns the current health representation.
     *
     * Kept for compatibility with existing code/tests.
     */
    public String getHealth(String gateway) {
        return getHealthScore(gateway) > 0.0
                ? "UP"
                : "DOWN";
    }

    /**
     * Returns whether the gateway currently has a
     * non-zero health score.
     */
    public boolean isHealthy(String gateway) {
        return getHealthScore(gateway) > 0.0;
    }

    public Duration getHealthTtl() {
        return HEALTH_TTL;
    }

    private String buildKey(String gateway) {
        return HEALTH_KEY_PREFIX + gateway;
    }
}
