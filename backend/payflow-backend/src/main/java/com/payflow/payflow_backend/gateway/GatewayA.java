package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GatewayA implements PaymentGateway {

    @Override
    public String getName() {
        return "GATEWAY_A";
    }

    @Override
    public boolean supports(String paymentMethod) {
        return "UPI".equalsIgnoreCase(paymentMethod)
                || "CARD".equalsIgnoreCase(paymentMethod);
    }

    @Override
    public GatewayResult processPayment(Transaction transaction) {

        /*
         * Simulation rule:
         * Amount 9999 causes Gateway A to fail.
         *
         * This lets us test failover without connecting
         * to a real payment provider.
         */
        if (transaction.getAmount().compareTo(
                new BigDecimal("9999.00")) == 0) {

            return GatewayResult.failure(
                    getName(),
                    "Gateway A simulated failure");
        }

        return GatewayResult.success(
                getName(),
                "Payment processed successfully by Gateway A");
    }
}