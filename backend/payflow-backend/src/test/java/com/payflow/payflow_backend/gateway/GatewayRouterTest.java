package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayRouterTest {

    private GatewayHealthService gatewayHealthService;

    private GatewayA gatewayA;
    private GatewayB gatewayB;

    private GatewayRouter gatewayRouter;

    @BeforeEach
    void setUp() {

        gatewayHealthService =
                mock(GatewayHealthService.class);

        gatewayA = new GatewayA();
        gatewayB = new GatewayB();

        gatewayRouter = new GatewayRouter(
                List.of(gatewayA, gatewayB),
                gatewayHealthService
        );
    }

    @Test
    void shouldIncludeSupportedGateway() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("UPI");

        when(gatewayHealthService.getHealthScore("GATEWAY_A"))
                .thenReturn(0.90);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(1, result.size());
        assertEquals(gatewayA, result.get(0));
    }

    @Test
    void shouldOrderSupportedGatewaysByHealthScore() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.getHealthScore("GATEWAY_A"))
                .thenReturn(0.20);

        when(gatewayHealthService.getHealthScore("GATEWAY_B"))
                .thenReturn(0.90);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(2, result.size());
        assertEquals(gatewayB, result.get(0));
        assertEquals(gatewayA, result.get(1));
    }

    @Test
    void shouldIncludeGatewayWithLowHealthScoreForFailover() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.getHealthScore("GATEWAY_A"))
                .thenReturn(0.10);

        when(gatewayHealthService.getHealthScore("GATEWAY_B"))
                .thenReturn(0.90);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(2, result.size());
        assertEquals(gatewayB, result.get(0));
        assertEquals(gatewayA, result.get(1));
    }

    @Test
    void shouldTreatGatewayWithoutHealthRecordAsHealthy() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.getHealthScore("GATEWAY_A"))
                .thenReturn(1.0);

        when(gatewayHealthService.getHealthScore("GATEWAY_B"))
                .thenReturn(1.0);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(2, result.size());
    }

    @Test
    void shouldThrowExceptionWhenPaymentMethodIsUnsupported() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("NETBANKING");

        assertThrows(
                IllegalArgumentException.class,
                () -> gatewayRouter.routeAll(transaction)
        );
    }

    @Test
    void shouldOrderGatewaysByHealthScore() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.getHealthScore("GATEWAY_A"))
                .thenReturn(0.70);

        when(gatewayHealthService.getHealthScore("GATEWAY_B"))
                .thenReturn(0.95);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(2, result.size());
        assertEquals(gatewayB, result.get(0));
        assertEquals(gatewayA, result.get(1));
    }

    @Test
    void routeShouldReturnHighestScoredSupportedGateway() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.getHealthScore("GATEWAY_A"))
                .thenReturn(0.70);

        when(gatewayHealthService.getHealthScore("GATEWAY_B"))
                .thenReturn(0.95);

        PaymentGateway result =
                gatewayRouter.route(transaction);

        assertEquals(
                gatewayB,
                result);
    }
}