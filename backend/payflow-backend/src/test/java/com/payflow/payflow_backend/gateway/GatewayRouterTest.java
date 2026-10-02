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
    void shouldIncludeHealthyGateway() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("UPI");

        when(gatewayHealthService.isHealthy("A"))
                .thenReturn(true);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(1, result.size());
        assertEquals(gatewayA, result.get(0));
    }

    @Test
    void shouldSkipUnhealthyGateway() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.isHealthy("A"))
                .thenReturn(false);

        when(gatewayHealthService.isHealthy("B"))
                .thenReturn(true);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(1, result.size());
        assertEquals(gatewayB, result.get(0));
    }

    @Test
    void shouldTreatGatewayWithoutHealthRecordAsHealthy() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.isHealthy("A"))
                .thenReturn(true);

        when(gatewayHealthService.isHealthy("B"))
                .thenReturn(true);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(2, result.size());
        assertEquals(gatewayA, result.get(0));
        assertEquals(gatewayB, result.get(1));
    }

    @Test
    void shouldThrowExceptionWhenPaymentMethodIsUnsupported() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("NETBANKING");

        when(gatewayHealthService.isHealthy("A"))
                .thenReturn(true);

        when(gatewayHealthService.isHealthy("B"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> gatewayRouter.routeAll(transaction)
        );
    }

    @Test
    void shouldPreserveGatewayOrderForFailover() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.isHealthy("A"))
                .thenReturn(true);

        when(gatewayHealthService.isHealthy("B"))
                .thenReturn(true);

        List<PaymentGateway> result =
                gatewayRouter.routeAll(transaction);

        assertEquals(2, result.size());
        assertEquals(gatewayA, result.get(0));
        assertEquals(gatewayB, result.get(1));
    }

    @Test
    void routeShouldReturnFirstHealthySupportedGateway() {

        Transaction transaction =
                mock(Transaction.class);

        when(transaction.getPaymentMethod())
                .thenReturn("CARD");

        when(gatewayHealthService.isHealthy("A"))
                .thenReturn(true);

        when(gatewayHealthService.isHealthy("B"))
                .thenReturn(true);

        PaymentGateway result =
                gatewayRouter.route(transaction);

        assertEquals(gatewayA, result);
    }
}