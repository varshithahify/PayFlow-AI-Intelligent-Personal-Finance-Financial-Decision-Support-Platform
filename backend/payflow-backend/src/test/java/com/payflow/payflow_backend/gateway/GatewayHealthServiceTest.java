package com.payflow.payflow_backend.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayHealthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private GatewayHealthService gatewayHealthService;

    @Test
    void shouldMarkGatewayAsHealthy() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        gatewayHealthService.setHealthy("A");

        verify(valueOperations)
                .set(
                        "gateway:health:A",
                        "UP",
                        gatewayHealthService.getHealthTtl()
                );
    }

    @Test
    void shouldMarkGatewayAsUnhealthy() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        gatewayHealthService.setUnhealthy("A");

        verify(valueOperations)
                .set(
                        "gateway:health:A",
                        "DOWN",
                        gatewayHealthService.getHealthTtl()
                );
    }

    @Test
    void shouldReturnHealthyGatewayStatus() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get("gateway:health:A"))
                .thenReturn("UP");

        String result =
                gatewayHealthService.getHealth("A");

        assertEquals("UP", result);
    }

    @Test
    void shouldReturnUnhealthyGatewayStatus() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get("gateway:health:A"))
                .thenReturn("DOWN");

        String result =
                gatewayHealthService.getHealth("A");

        assertEquals("DOWN", result);
    }

    @Test
    void shouldTreatUnknownGatewayAsHealthyByDefault() {

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.get("gateway:health:A"))
                .thenReturn(null);

        boolean result =
                gatewayHealthService.isHealthy("A");

        assertTrue(result);
    }
}