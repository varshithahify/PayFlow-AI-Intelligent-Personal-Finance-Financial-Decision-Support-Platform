package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.Comparator;
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
                        new IllegalStateException(
                                "No supported payment gateway available"));
    }

    public List<PaymentGateway> routeAll(
            Transaction transaction) {

        List<PaymentGateway> supportedGateways =
                gateways.stream()
                        .filter(gateway ->
                                gateway.supports(
                                        transaction.getPaymentMethod()))
                        .sorted(
                                Comparator.comparingDouble(
                                        this::getGatewayHealthScore)
                                        .reversed())
                        .toList();

        if (supportedGateways.isEmpty()) {
            throw new IllegalArgumentException(
                    "No supported payment gateway available");
        }

        return supportedGateways;
    }

    public PaymentGateway getGatewayByName(
            String gatewayName) {

        return gateways.stream()
                .filter(gateway ->
                        gateway.getName().equalsIgnoreCase(gatewayName))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment gateway not found: "
                                        + gatewayName));
    }

    private double getGatewayHealthScore(
            PaymentGateway gateway) {

        return gatewayHealthService.getHealthScore(
                gateway.getName());
    }
}