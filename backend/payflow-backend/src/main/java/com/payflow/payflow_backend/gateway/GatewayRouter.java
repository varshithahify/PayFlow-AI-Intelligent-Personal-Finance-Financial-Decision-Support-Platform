package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatewayRouter {

    private final List<PaymentGateway> gateways;
    private final GatewayHealthService gatewayHealthService;

    public GatewayRouter(
            List<PaymentGateway> gateways,
            GatewayHealthService gatewayHealthService) {

        this.gateways = gateways;
        this.gatewayHealthService = gatewayHealthService;
    }

    public PaymentGateway route(Transaction transaction) {

        return routeAll(transaction)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No healthy payment gateway supports payment method: "
                                        + transaction.getPaymentMethod()));
    }

    public List<PaymentGateway> routeAll(
            Transaction transaction) {

        List<PaymentGateway> supportedAndHealthyGateways =
                gateways.stream()
                        .filter(gateway ->
                                gateway.supports(
                                        transaction.getPaymentMethod()))
                        .filter(this::isGatewayHealthy)
                        .toList();

        if (supportedAndHealthyGateways.isEmpty()) {

            throw new IllegalArgumentException(
                    "No healthy payment gateway supports payment method: "
                            + transaction.getPaymentMethod());
        }

        return supportedAndHealthyGateways;
    }

    private boolean isGatewayHealthy(
            PaymentGateway gateway) {

        String gatewayName =
                gateway.getClass()
                        .getSimpleName()
                        .replace("Gateway", "");

        return gatewayHealthService.isHealthy(
                gatewayName);
    }
}