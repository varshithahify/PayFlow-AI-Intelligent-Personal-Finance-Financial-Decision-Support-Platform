package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.stereotype.Component;

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

        return GatewayResult.success(
                getName(),
                "Payment processed successfully by Gateway A"
        );
    }
}