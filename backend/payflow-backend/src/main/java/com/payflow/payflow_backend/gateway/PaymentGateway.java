package com.payflow.payflow_backend.gateway;

import com.payflow.payflow_backend.entity.Transaction;

public interface PaymentGateway {

    String getName();

    boolean supports(String paymentMethod);

    GatewayResult processPayment(Transaction transaction);
}