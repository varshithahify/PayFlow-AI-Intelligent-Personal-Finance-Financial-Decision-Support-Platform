package com.payflow.payflow_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow_backend.dto.TransactionRequest;
import com.payflow.payflow_backend.dto.TransactionResponse;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.entity.TransactionStatus;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    private static final String MERCHANT_EMAIL = "merchant@example.com";
    private static final String IDEMPOTENCY_KEY = "test-idempotency-key";

    @Autowired
    private MockMvc mockMvc;

   private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(
                MERCHANT_EMAIL,
                null,
                List.of()
        );
    }

    private TransactionResponse createResponse(Long id) {

        Transaction transaction = new Transaction(
                new BigDecimal("100.00"),
                "INR",
                "CARD",
                TransactionStatus.SUCCESS,
                10L,
                1L,
                IDEMPOTENCY_KEY
        );

        transaction.setId(id);

        LocalDateTime now = LocalDateTime.now();

        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        return new TransactionResponse(transaction);
    }

    private TransactionRequest validRequest() {

        TransactionRequest request = new TransactionRequest();

        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");
        request.setPaymentMethod("CARD");

        return request;
    }

    // --------------------------------------------------
    // PAYMENT INITIATION TESTS
    // --------------------------------------------------

    @Test
    void initiatePaymentShouldReturnOk() throws Exception {

        TransactionResponse createdResponse = createResponse(1L);
        TransactionResponse processedResponse = createResponse(1L);

        when(transactionService.createTransaction(
                any(TransactionRequest.class),
                eq(MERCHANT_EMAIL),
                eq(IDEMPOTENCY_KEY)
        )).thenReturn(createdResponse);

        when(transactionService.startProcessing(
                eq(1L),
                eq(MERCHANT_EMAIL)
        )).thenReturn(processedResponse);

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(validRequest()))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.amount").value(100.00))
        .andExpect(jsonPath("$.currency").value("INR"))
        .andExpect(jsonPath("$.paymentMethod").value("CARD"))
        .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(transactionService).createTransaction(
                any(TransactionRequest.class),
                eq(MERCHANT_EMAIL),
                eq(IDEMPOTENCY_KEY)
        );

        verify(transactionService).startProcessing(
                eq(1L),
                eq(MERCHANT_EMAIL)
        );
    }

    @Test
    void initiatePaymentShouldRejectNullAmount() throws Exception {

        TransactionRequest request = validRequest();
        request.setAmount(null);

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void initiatePaymentShouldRejectZeroAmount() throws Exception {

        TransactionRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void initiatePaymentShouldRejectNegativeAmount() throws Exception {

        TransactionRequest request = validRequest();
        request.setAmount(new BigDecimal("-10.00"));

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void initiatePaymentShouldRejectBlankCurrency() throws Exception {

        TransactionRequest request = validRequest();
        request.setCurrency(" ");

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void initiatePaymentShouldRejectBlankPaymentMethod() throws Exception {

        TransactionRequest request = validRequest();
        request.setPaymentMethod(" ");

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void initiatePaymentShouldRejectMissingIdempotencyKey() throws Exception {

        mockMvc.perform(
                post("/api/v1/payments/initiate")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    // --------------------------------------------------
    // GET ALL PAYMENTS TESTS
    // --------------------------------------------------

    @Test
    void getPaymentsShouldReturnUserTransactions() throws Exception {

        when(transactionService.getUserTransactions(MERCHANT_EMAIL))
                .thenReturn(List.of(
                        createResponse(1L),
                        createResponse(2L)
                ));

        mockMvc.perform(
                get("/api/v1/payments")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));

        verify(transactionService).getUserTransactions(MERCHANT_EMAIL);
    }

    @Test
    void getPaymentsShouldReturnEmptyListWhenNoPaymentsExist() throws Exception {

        when(transactionService.getUserTransactions(MERCHANT_EMAIL))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/v1/payments")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    }

    // --------------------------------------------------
    // GET PAYMENT BY ID TESTS
    // --------------------------------------------------

    @Test
    void getPaymentShouldReturnTransaction() throws Exception {

        when(transactionService.getTransaction(1L, MERCHANT_EMAIL))
                .thenReturn(createResponse(1L));

        mockMvc.perform(
                get("/api/v1/payments/1")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.currency").value("INR"));

        verify(transactionService).getTransaction(1L, MERCHANT_EMAIL);
    }

    @Test
    void getPaymentShouldReturnNotFoundWhenTransactionDoesNotExist()
            throws Exception {

        when(transactionService.getTransaction(999L, MERCHANT_EMAIL))
                .thenThrow(new com.payflow.payflow_backend.exception.ResourceNotFoundException(
                        "Transaction not found"
                ));

        mockMvc.perform(
                get("/api/v1/payments/999")
                        .principal(authentication())
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Transaction not found"));
    }

    // --------------------------------------------------
    // REFUND PAYMENT TESTS
    // --------------------------------------------------

    @Test
    void refundPaymentShouldReturnRefundedTransaction() throws Exception {

        TransactionResponse refundedResponse = createResponse(1L);

        when(transactionService.refundTransaction(1L, MERCHANT_EMAIL))
                .thenReturn(refundedResponse);

        mockMvc.perform(
                post("/api/v1/payments/1/refund")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

        verify(transactionService).refundTransaction(1L, MERCHANT_EMAIL);
    }

    @Test
    void refundPaymentShouldReturnConflictWhenRefundIsInvalid()
            throws Exception {

        when(transactionService.refundTransaction(1L, MERCHANT_EMAIL))
                .thenThrow(new IllegalStateException(
                        "Transaction cannot be refunded"
                ));

        mockMvc.perform(
                post("/api/v1/payments/1/refund")
                        .principal(authentication())
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error")
                .value("Transaction cannot be refunded"));
    }
}