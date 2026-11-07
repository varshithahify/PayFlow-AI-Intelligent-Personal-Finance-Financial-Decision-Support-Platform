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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    private static final String MERCHANT_EMAIL =
            "merchant@example.com";

    private static final String IDEMPOTENCY_KEY =
            "test-idempotency-key";

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
    // CREATE TRANSACTION TESTS
    // --------------------------------------------------

    @Test
    void shouldCreateTransactionSuccessfully() throws Exception {

        TransactionResponse response = createResponse(1L);

        when(transactionService.createTransaction(
                any(TransactionRequest.class),
                eq(MERCHANT_EMAIL),
                eq(IDEMPOTENCY_KEY)
        )).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions")
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
    }


    @Test
    void shouldRejectTransactionWhenIdempotencyKeyIsMissing()
            throws Exception {

        mockMvc.perform(
                post("/api/transactions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectTransactionWhenAmountIsZero()
            throws Exception {

        TransactionRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(
                post("/api/transactions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectTransactionWhenCurrencyIsBlank()
            throws Exception {

        TransactionRequest request = validRequest();
        request.setCurrency("");

        mockMvc.perform(
                post("/api/transactions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectTransactionWhenPaymentMethodIsBlank()
            throws Exception {

        TransactionRequest request = validRequest();
        request.setPaymentMethod("");

        mockMvc.perform(
                post("/api/transactions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    // --------------------------------------------------
    // GET TRANSACTIONS TESTS
    // --------------------------------------------------

    @Test
    void shouldReturnAllUserTransactions() throws Exception {

        List<TransactionResponse> responses = List.of(
                createResponse(1L),
                createResponse(2L)
        );

        when(transactionService.getUserTransactions(
                MERCHANT_EMAIL
        )).thenReturn(responses);

        mockMvc.perform(
                get("/api/transactions")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));

        verify(transactionService).getUserTransactions(
                MERCHANT_EMAIL
        );
    }


    @Test
    void shouldReturnEmptyListWhenUserHasNoTransactions()
            throws Exception {

        when(transactionService.getUserTransactions(
                MERCHANT_EMAIL
        )).thenReturn(List.of());

        mockMvc.perform(
                get("/api/transactions")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).getUserTransactions(
                MERCHANT_EMAIL
        );
    }


    // --------------------------------------------------
    // PAGINATION AND FILTERING TESTS
    // --------------------------------------------------

    @Test
    void shouldReturnPaginatedTransactionsWithDefaultValues()
            throws Exception {

        TransactionResponse response = createResponse(1L);

        Page<TransactionResponse> result = new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 10),
                1
        );

        when(transactionService.getUserTransactions(
                eq(MERCHANT_EMAIL),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(result);

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1));

        verify(transactionService).getUserTransactions(
                eq(MERCHANT_EMAIL),
                isNull(),
                isNull(),
                isNull(),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 10
                        && pageable.getSort().getOrderFor("createdAt")
                                .getDirection() == Sort.Direction.DESC
                )
        );
    }


    @Test
    void shouldApplyPaginationFiltersAndSorting()
            throws Exception {

        Page<TransactionResponse> result = new PageImpl<>(
                List.of(createResponse(1L)),
                PageRequest.of(
                        1,
                        5,
                        Sort.by(Sort.Direction.ASC, "amount")
                ),
                1
        );

        when(transactionService.getUserTransactions(
                eq(MERCHANT_EMAIL),
                eq("refund"),
                eq(TransactionStatus.SUCCESS),
                eq("UPI"),
                any(Pageable.class)
        )).thenReturn(result);

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", "refund")
                        .param("status", " success ")
                        .param("paymentMethod", " upi ")
                        .param("sortBy", "amount")
                        .param("direction", "asc")
        )
        .andExpect(status().isOk());

        verify(transactionService).getUserTransactions(
                eq(MERCHANT_EMAIL),
                eq("refund"),
                eq(TransactionStatus.SUCCESS),
                eq("UPI"),
                argThat(pageable ->
                        pageable.getPageNumber() == 1
                        && pageable.getPageSize() == 5
                        && pageable.getSort().getOrderFor("amount")
                                .getDirection() == Sort.Direction.ASC
                )
        );
    }


    @Test
    void shouldRejectNegativePageNumber() throws Exception {

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("page", "-1")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectPageSizeBelowMinimum() throws Exception {

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("size", "0")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectPageSizeAboveMaximum() throws Exception {

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("size", "101")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectInvalidSortField() throws Exception {

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("sortBy", "password")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectInvalidSortDirection() throws Exception {

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("direction", "random")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldRejectInvalidTransactionStatus() throws Exception {

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("status", "INVALID")
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }


    @Test
    void shouldIgnoreBlankStatusAndPaymentMethod()
            throws Exception {

        Page<TransactionResponse> result = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(transactionService.getUserTransactions(
                eq(MERCHANT_EMAIL),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(result);

        mockMvc.perform(
                get("/api/transactions/page")
                        .principal(authentication())
                        .param("status", " ")
                        .param("paymentMethod", " ")
        )
        .andExpect(status().isOk());

        verify(transactionService).getUserTransactions(
                eq(MERCHANT_EMAIL),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }


    // --------------------------------------------------
    // GET TRANSACTION BY ID TEST
    // --------------------------------------------------

    @Test
    void shouldReturnTransactionById() throws Exception {

        TransactionResponse response = createResponse(10L);

        when(transactionService.getTransaction(
                10L,
                MERCHANT_EMAIL
        )).thenReturn(response);

        mockMvc.perform(
                get("/api/transactions/10")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10));

        verify(transactionService).getTransaction(
                10L,
                MERCHANT_EMAIL
        );
    }


    // --------------------------------------------------
    // TRANSACTION STATE TESTS
    // --------------------------------------------------

    @Test
    void shouldStartTransactionProcessing() throws Exception {

        TransactionResponse response = createResponse(10L);

        when(transactionService.startProcessing(
                10L,
                MERCHANT_EMAIL
        )).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions/10/process")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10));

        verify(transactionService).startProcessing(
                10L,
                MERCHANT_EMAIL
        );
    }


    @Test
    void shouldMarkTransactionAsSuccessful() throws Exception {

        TransactionResponse response = createResponse(10L);

        when(transactionService.markSuccess(
                10L,
                MERCHANT_EMAIL
        )).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions/10/success")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(transactionService).markSuccess(
                10L,
                MERCHANT_EMAIL
        );
    }


    @Test
    void shouldMarkTransactionAsFailed() throws Exception {

        TransactionResponse response = createResponse(10L);

        when(transactionService.markFailed(
                10L,
                MERCHANT_EMAIL
        )).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions/10/fail")
                        .principal(authentication())
        )
        .andExpect(status().isOk());

        verify(transactionService).markFailed(
                10L,
                MERCHANT_EMAIL
        );
    }


    @Test
    void shouldRefundTransaction() throws Exception {

        TransactionResponse response = createResponse(10L);

        when(transactionService.refundTransaction(
                10L,
                MERCHANT_EMAIL
        )).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions/10/refund")
                        .principal(authentication())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10));

        verify(transactionService).refundTransaction(
                10L,
                MERCHANT_EMAIL
        );
    }


    // --------------------------------------------------
    // DELETE TRANSACTION TEST
    // --------------------------------------------------

    @Test
    void shouldDeleteTransactionSuccessfully() throws Exception {

        doNothing().when(transactionService).deleteTransaction(
                10L,
                MERCHANT_EMAIL
        );

        mockMvc.perform(
                delete("/api/transactions/10")
                        .principal(authentication())
        )
        .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(
                10L,
                MERCHANT_EMAIL
        );
    }
}