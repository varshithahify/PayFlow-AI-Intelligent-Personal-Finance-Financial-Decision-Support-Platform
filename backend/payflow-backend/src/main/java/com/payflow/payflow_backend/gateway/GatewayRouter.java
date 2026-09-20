package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatewayRouter {

    private final List<PaymentGateway> gateways;

    public GatewayRouter(List<PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public PaymentGateway route(Transaction transaction) {

        return gateways.stream()
                .filter(gateway ->
                        gateway.supports(
                                transaction.getPaymentMethod()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No payment gateway supports payment method: "
                                        + transaction.getPaymentMethod()));
    }

    public List<PaymentGateway> routeAll(
            Transaction transaction) {

        List<PaymentGateway> supportedGateways =
                gateways.stream()
                        .filter(gateway ->
                                gateway.supports(
                                        transaction.getPaymentMethod()))
                        .toList();

        if (supportedGateways.isEmpty()) {
            throw new IllegalArgumentException(
                    "No payment gateway supports payment method: "
                            + transaction.getPaymentMethod());
        }

        return supportedGateways;
    }
}