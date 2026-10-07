package com.payflow.payflow_backend.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayHealthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private GatewayHealthService gatewayHealthService;

    @Test
    void shouldRecordSuccessfulGatewayResult() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        gatewayHealthService.recordResult(
                "A",
                true,
                100);

        verify(hashOperations)
                .increment(
                        "gateway:health:A",
                        "success",
                        1);

        verify(hashOperations)
                .increment(
                        "gateway:health:A",
                        "total",
                        1);

        verify(hashOperations)
                .increment(
                        "gateway:health:A",
                        "total_latency",
                        100);

        verify(redisTemplate)
                .expire(
                        "gateway:health:A",
                        gatewayHealthService.getHealthTtl());
    }

    @Test
    void shouldRecordFailedGatewayResult() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        gatewayHealthService.recordResult(
                "A",
                false,
                300);

        verify(hashOperations)
                .increment(
                        "gateway:health:A",
                        "failure",
                        1);

        verify(hashOperations)
                .increment(
                        "gateway:health:A",
                        "total",
                        1);

        verify(hashOperations)
                .increment(
                        "gateway:health:A",
                        "total_latency",
                        300);

        verify(redisTemplate)
                .expire(
                        "gateway:health:A",
                        gatewayHealthService.getHealthTtl());
    }

    @Test
    void shouldCalculateGatewayHealthScore() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        Map<Object, Object> data = Map.of(
                "success", "8",
                "failure", "2",
                "total", "10",
                "total_latency", "1000"
        );

        when(hashOperations.entries("gateway:health:A"))
                .thenReturn(data);

        double score =
                gatewayHealthService.getHealthScore("A");

        /*
         * Success rate = 8 / 10 = 0.8
         *
         * Average latency = 1000 / 10 = 100 ms
         *
         * Latency score =
         * 1 - (100 / 2000) = 0.95
         *
         * Final score =
         * (0.8 * 0.6) + (0.95 * 0.4)
         * = 0.86
         */
        assertEquals(
                0.86,
                score,
                0.0001);
    }

    @Test
    void shouldReturnDefaultHealthScoreForUnknownGateway() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        when(hashOperations.entries("gateway:health:A"))
                .thenReturn(Map.of());

        double score =
                gatewayHealthService.getHealthScore("A");

        assertEquals(
                1.0,
                score,
                0.0001);
    }

    @Test
    void shouldMarkGatewayAsHealthyWithoutWritingStringValue() {

        gatewayHealthService.setHealthy("A");

        /*
         * Health is now score-based.
         * setHealthy() must not create a Redis string key.
         */
        verify(redisTemplate, org.mockito.Mockito.never())
                .opsForValue();
    }

    @Test
    void shouldMarkGatewayAsUnhealthyWithoutWritingStringValue() {

        gatewayHealthService.setUnhealthy("A");

        /*
         * Health is now score-based.
         * setUnhealthy() must not create a Redis string key.
         */
        verify(redisTemplate, org.mockito.Mockito.never())
                .opsForValue();
    }

    @Test
    void shouldReturnHealthyGatewayStatusWhenScoreIsPositive() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        when(hashOperations.entries("gateway:health:A"))
                .thenReturn(Map.of(
                        "success", "8",
                        "failure", "2",
                        "total", "10",
                        "total_latency", "1000"
                ));

        String result =
                gatewayHealthService.getHealth("A");

        assertEquals("UP", result);
    }

    @Test
    void shouldReturnUnhealthyGatewayStatusWhenScoreIsZero() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        when(hashOperations.entries("gateway:health:A"))
                .thenReturn(Map.of(
                        "success", "0",
                        "failure", "1",
                        "total", "1",
                        "total_latency", "2000"
                ));

        String result =
                gatewayHealthService.getHealth("A");

        assertEquals("DOWN", result);
    }

    @Test
    void shouldTreatUnknownGatewayAsHealthyByDefault() {

        when(redisTemplate.opsForHash())
                .thenReturn(hashOperations);

        when(hashOperations.entries("gateway:health:A"))
                .thenReturn(Map.of());

        boolean result =
                gatewayHealthService.isHealthy("A");

        assertTrue(result);
    }
}