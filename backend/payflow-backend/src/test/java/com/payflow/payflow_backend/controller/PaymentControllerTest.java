package com.payflow.payflow_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void initiatePaymentShouldReturnOk() throws Exception {

        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");
        request.setPaymentMethod("CARD");

        TransactionResponse response =
                mock(TransactionResponse.class);

        when(transactionService.createTransaction(
                any(TransactionRequest.class),
                eq("merchant@example.com"),
                eq("test-idempotency-key")))
                .thenReturn(response);

        when(transactionService.startProcessing(
                any(Long.class),
                eq("merchant@example.com")))
                .thenReturn(response);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "merchant@example.com",
                        null,
                        List.of());

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(
                                "Idempotency-Key",
                                "test-idempotency-key")
                        .content(
                                objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());
    }
}