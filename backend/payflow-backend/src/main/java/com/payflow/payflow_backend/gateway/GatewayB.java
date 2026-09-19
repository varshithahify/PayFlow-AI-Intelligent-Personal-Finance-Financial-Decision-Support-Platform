package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class GatewayB implements PaymentGateway {

    @Override
    public String getName() {
        return "GATEWAY_B";
    }

    @Override
    public boolean supports(String paymentMethod) {
        return "CARD".equalsIgnoreCase(paymentMethod);
    }

    @Override
    public GatewayResult processPayment(Transaction transaction) {

        return GatewayResult.success(
                getName(),
                "Payment processed successfully by Gateway B"
        );
    }
}